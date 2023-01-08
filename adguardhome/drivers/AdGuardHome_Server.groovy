/*
 *  AdGuard Home DNS Server Driver for Hubitat
 *  Author: JoKneeMo <https://github.com/JoKneeMo>
 *  Copyright: JoKneeMo <https://github.com/JoKneeMo>
 *  License: GPL-3.0-only
 *  Version: 1.1.1

-------------------------------------------

Change history:

1.1.1 - JoKneeMo - Set attribute types to string because bool is not valid
1.1.0 - JoKneeMo - Added ability to create a single client
                 - Fix "null" value from being added to block lists
1.0.1 - JoKneeMo - Fix unsupported media type error
1.0.0 - JoKneeMo - Initial release
*/

metadata {
    definition(name: "AdGuard Home DNS Server", namespace: "JoKneeMo", author: "JoKneeMo", importUrl: "") {
        capability "Initialize"
        capability "Refresh"
        command "initialize"
        command "refresh"
        command "createChildDevices"
        command "createSingleChild", ["String"]
        command "deleteChildDevices"
        command "ProtectionOn"
        command "ProtectionOff"
        command "FilteringOn"
        command "FilteringOff"
        command "DhcpOn"
        command "DhcpOff"
        command "ParentalControlOn"
        command "ParentalControlOff"
        command "SafeBrowsingOn"
        command "SafeBrowsingOff"
        command "SafeSearchOn"
        command "SafeSearchOff"
        command "blockService", ["String"]
        command "unblockService", ["String"]
        attribute "version", "string"
        attribute "protection", "string"
        attribute "filtering", "string"
        attribute "dhcp", "string"
        attribute "parental", "string"
        attribute "safeBrowsing", "string"
        attribute "safeSearch", "string"
        attribute "blockedServices", "string"
    }
}

preferences {
    section {
        input name: "serverIP", type: "text", title: "Server IP Address", required: true
        input name: "username", type: "text", title: "Username", required: true
        input name: "password", type: "password", title: "Password", required: true
        input name: "tlsEnable", type: "bool", title: "TLS Connection", defaultValue: false
    }
    section {
        input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "logTrace", type: "bool", title: "Enable trace logging", defaultValue: false
    }
}

def initialize() {
    refresh()
    //runEvery5Minutes("refresh")
}

def refresh() {
    logDebug "DNS Server Polling..."
    if (serverIP == null) {
        logError "Server IP/FQDN not entered in preferences"
        return
    }
    getStatus()
    getFiltering()
    getDHCP()
    getSafeBrowsing()
    getSafeSearch()
    getParental()
    getBlockedServices()
}

// State Collectors
def getStatus() {
    logDebug "Getting Status...."
    def respValues = doHttpGet("/status", null)
    sendEvent(name: "version", value: respValues.version)
    sendEvent(name: "protection", value: respValues.protection_enabled)
}

def getFiltering() {
    logDebug "Getting Filtering...."
    def respValues = doHttpGet("/filtering/status", null)
    sendEvent(name: "filtering", value: respValues.enabled)
}

def getDHCP() {
    logDebug "Getting DHCP...."
    def respValues = doHttpGet("/dhcp/status", null)
    sendEvent(name: "dhcp", value: respValues.enabled)
}

def getSafeBrowsing() {
    logDebug "Getting SafeBrowsing...."
    def respValues = doHttpGet("/safebrowsing/status", null)
    sendEvent(name: "safeBrowsing", value: respValues.enabled)
}

def getSafeSearch() {
    logDebug "Getting SafeSearch...."
    def respValues = doHttpGet("/safesearch/status", null)
    sendEvent(name: "safeSearch", value: respValues.enabled)
}

def getParental() {
    logDebug "Getting Parental...."
    def respValues = doHttpGet("/parental/status", null)
    sendEvent(name: "parental", value: respValues.enabled)
}

def getBlockedServices() {
    logDebug "Getting Blocked Services...."
    def respValues = doHttpGet("/blocked_services/list", null)
    respValues.remove("null")
    sendEvent(name: "blockedServices", value: respValues)
}


// State Controls
def ProtectionOn() {
    logDebug "Enabling Protection...."
    httpPayload = [protection_enabled: true]
    doHttpPostJson("/dns_config", httpPayload)
    getStatus()
}

def ProtectionOff() {
    logDebug "Disabling Protection...."
    httpPayload = [protection_enabled: false]
    doHttpPostJson("/dns_config", httpPayload)
    getStatus()
}

def FilteringOn() {
    logDebug "Enabling Filtering...."
    httpPayload = [enabled: true, interval: 24]
    doHttpPostJson("/filtering/config", httpPayload)
    getFiltering()
}

def FilteringOff() {
    logDebug "Disabling Filtering...."
    httpPayload = [enabled: false, interval: 24]
    doHttpPostJson("/filtering/config", httpPayload)
    getFiltering()
}

def DhcpOn() {
    logDebug "Enabling DHCP...."
    httpPayload = [enabled: true]
    doHttpPostJson("/dhcp/set_config", httpPayload)
    getDHCP()
}

def DhcpOff() {
    logDebug "Disabling DHCP...."
    httpPayload = [enabled: false]
    doHttpPostJson("/dhcp/set_config", httpPayload)
    getDHCP()
}

def ParentalControlOn() {
    logDebug "Enabling Parental Control...."
    httpPayload = [sensitivity: "TEEN"]
    doHttpPostJson("/parental/enable", httpPayload)
    getParental()
}

def ParentalControlOff() {
    logDebug "Disabling Parental Control...."
    doHttpPostJson("/parental/disable", "")
    getParental()
}

def SafeBrowsingOn() {
    logDebug "Enabling SafeBrowsing...."
    doHttpPostJson("/safebrowsing/enable", "")
    getSafeBrowsing()
}

def SafeBrowsingOff() {
    logDebug "Disabling SafeBrowsing...."
    doHttpPostJson("/safebrowsing/disable", "")
    getSafeBrowsing()
}

def SafeSearchOn() {
    logDebug "Enabling SafeSearch...."
    doHttpPostJson("/safesearch/enable", "")
    getSafeSearch()
}

def SafeSearchOff() {
    logDebug "Disabling SafeSearch...."
    doHttpPostJson("/safesearch/disable", "")
    getSafeSearch()
}

def blockService(services_string) {
    List<String> services_list = convertMapString(services_string)
    services_list.remove("null")
    if(services_list.size() >= 1) {
        logDebug "Blocking ${services_list.size()} Service(s): ${services_list}..."
        List<String> currentBlocks_list = convertMapString(device.currentValue("blockedServices"))
        logDebug "Currently Blocking ${currentBlocks_list.size()} Services: ${currentBlocks_list}"

        def postBlockList = []
        if ("${currentBlocks_list[0]}" != "") {
            def dedupBlockList = services_list - currentBlocks_list
            postBlockList += currentBlocks_list + dedupBlockList
        } else {
            postBlockList += services_list
        }
        postBlockList.remove("null")
        logDebug "Setting ${postBlockList.size} Blocked Services: ${postBlockList}"

        doHttpPostJson("/blocked_services/set", postBlockList)
        getBlockedServices()
    } else {
        logError "No new services to block [${services_list.toString}]"
    }
}

def unblockService(services_string) {
    List<String> services_list = convertMapString(services_string)
    services_list.remove("null")
    if(services_list.size() >= 1) {
        logDebug "Unblocking ${services_list.size()} Service(s): ${services_list}..."
        List<String> currentBlocks_list = convertMapString(device.currentValue("blockedServices"))
        logDebug "Currently Blocking ${currentBlocks_list.size()} Services: ${currentBlocks_list}"

        def postBlockList = currentBlocks_list - services_list
        postBlockList.remove("null")
        logDebug "Setting ${postBlockList.size} Blocked Services: ${postBlockList}"

        doHttpPostJson("/blocked_services/set", postBlockList)
        getBlockedServices()
    } else {
        logError "No new services to unblock [${services_list.toString}]"
    }
}


// Client Controls
def getClients() {
    logDebug "Getting Clients...."
    def respValues = doHttpGet("/clients", null)
    logTrace "Clients Response: ${respValues.clients}"
    return respValues
}

def createChildDevices() {
    (getClients()?.clients)?.each {
        if((it.name && it.ids[0]) && !findChildDevice(it.ids[0], "client")) {
            createChildDevice(it.name, it.ids, "client")
        } else {
            logTrace "Client Already Exists: ${it.name} | ${it.ids[0]}"
        }
    }
}

def createSingleChild(id) {
    logDebug "Adding Single Client [${id}]...."
    def clientQuery = doHttpGet("/clients/find?ip0=${id}", null)["${id}"][0]
    logTrace "Client Query Response: ${clientQuery}"
    if(clientQuery.name != "") {
        if(!findChildDevice(clientQuery.ids[0], "client")) {
            createChildDevice(clientQuery.name, clientQuery.ids, "client")
        } else {
            logError "Client Already Exists: ${it.name} | ${it.ids[0]}"
        }
    } else {
        logError "Client with ID ${id} does not exist on server."
    }
}

def createChildDevice(name, ids, deviceType) {
    def child
        try {
            switch(deviceType.toString()) {
                case "client":
                    logDebug "Creating New Client: ${name} | ${ids[0]}"
                    newChild = addChildDevice("AdGuard Home DNS Client", childDNI(ids[0], deviceType), [name: childName(name, deviceType), label: childName(name, deviceType), isComponent: false])
                    newChild.parentEvent("clientIds", ids)
                    pauseExecution(1000)
                    newChild.refresh()
                    break
                default:
                    logDebug "deviceType is not specified or is unsupported"
            }
        }
        catch (Exception e) {
            logError "Child Device Creation Failed: ${e.message}"
        }
}

def deleteChildDevices() {
    logInfo "Deleting all child devices...."
    for(child in getChildDevices()) {
        deleteChildDevice(child.deviceNetworkId)
    }
}

def childName(name, deviceType) {
    return "DNS ${deviceType.toString().toUpperCase()} | ${name.toString()}"
}

def findChildDevice(id, deviceType) {
    return getChildDevice(childDNI(id, deviceType))
}

def childDNI(id, deviceType) {
    return "AGH_${deviceType.toString().toUpperCase()}|${id.toString()}"
}

// General Functions
def doHttpGet(endpoint,bodyData) {
    logDebug "Getting ${endpoint}...."
    def httpParams = [
        uri: "${getBaseURI()}${endpoint}",
        ignoreSSLIssues: true,
        headers: getDefaultHeaders(),
        body : "${bodyData}"
    ]
    try {
        logTrace "HTTP Params: ${httpParams}"
        httpGet(httpParams) { resp ->
            logTrace "Response Data: ${resp.getData().toString()}"
            def respValues = resp.getData()
            return respValues
        }
    } catch(Exception e) {
        logError "HTTP Error: ${e}"
    }
}

def doHttpPostJson(endpoint,bodyData) {
    logDebug "Posting to ${endpoint}...."
    def httpParams = [
        uri: "${getBaseURI()}${endpoint}",
        ignoreSSLIssues: true,
        headers: getDefaultHeaders(),
        body : bodyData
    ]
    try {
        logTrace "HTTP Params: ${httpParams}"
        httpPostJson(httpParams) { resp ->
            logTrace "Response Data: ${resp.getData().toString()}"
            def respValues = resp.getData()
            return respValues
        }
    } catch(Exception e) {
        logError "HTTP Error: ${e}"
    }
}

def getBaseURI() {
    if (tlsEnable) {
        baseProtocol = "https://"
    } else {
        baseProtocol = "http://"
    }

    baseURI = "${baseProtocol}${serverIP}/control"
    
    logTrace "Base URI: ${baseURI.toString()}"
    return baseURI.toString()
}

def getDefaultHeaders() {
    def headers = [:]
    headers.put("Accept-Encoding", "gzip, deflate, br")
    headers.put("Connection", "keep-alive")
    headers.put("Accept", "application/json, text/plain, */*")
    headers.put("Content-type", "application/json")
    headers.put("User-Agent", "Hubitat")
    authString="${username}:${password}"
    headers.put("Authorization", "Basic ${authString.bytes.encodeBase64().toString()}")

    logTrace "Headers: ${headers}"
    return headers
}

def convertMapString(map) {
    return map.toString().replaceAll("\\[|\\]", "").split("\\s*,\\s*")
}

//logging help methods
private logError(msg) {
    log.error msg
}

private logWarn(msg) {
    log.warn msg
}

private logInfo(msg) {
    if (descriptionTextEnable) log.info msg
}

private logDebug(msg) {
    if (logDebug) log.debug msg
}

private logTrace(msg) {
    if (logTrace) log.trace msg
}
