/*
 *  AdGuard Home DNS Client for Hubitat
 *  Author: JoKneeMo <https://github.com/JoKneeMo>
 *  Copyright: JoKneeMo <https://github.com/JoKneeMo>
 *  License: GPL-3.0-only
 *  Version: 1.1.0

-------------------------------------------

Change history:

1.1.0 - JoKneeMo - Fix "null" value from being added to block lists
1.0.0 - JoKneeMo - Initial release
*/

import groovy.json.JsonOutput

metadata {
    definition(name: "AdGuard Home DNS Client", namespace: "JoKneeMo", author: "JoKneeMo", importUrl: "") {
        capability "Refresh"
        command "refresh"
        command "GlobalSettingsOn"
        command "GlobalSettingsOff"
        command "FilteringOn"
        command "FilteringOff"
        command "ParentalControlOn"
        command "ParentalControlOff"
        command "SafeBrowsingOn"
        command "SafeBrowsingOff"
        command "SafeSearchOn"
        command "SafeSearchOff"
        command "GlobalBlockListOn"
        command "GlobalBlockListOff"
        command "blockService", ["String"]
        command "unblockService", ["String"]
        attribute "name", "string"
        attribute "filtering", "bool"
        attribute "parental", "bool"
        attribute "safeBrowsing", "bool"
        attribute "safeSearch", "bool"
        attribute "blockedServices", "string"
        attribute "use_global_blocked_services", "bool"
        attribute "use_global_settings", "bool"
        attribute "clientIds", "string"
        attribute "clientTags", "string"
    }
}

preferences {
    section {
        input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "logTrace", type: "bool", title: "Enable trace logging", defaultValue: false
    }
}

def refresh() {
    logDebug "DNS Client Polling..."
    getStatus()
}

def formatMap() {
    def attribMap = [
            name: (device.currentValue("name")),
            data: [
                name: (device.currentValue("name")),
                filtering_enabled: (device.currentValue("filtering").toBoolean()),
                parental_enabled: (device.currentValue("parental").toBoolean()),
                safebrowsing_enabled: (device.currentValue("safeBrowsing").toBoolean()),
                safesearch_enabled: (device.currentValue("safeSearch").toBoolean()),
                use_global_blocked_services: (device.currentValue("use_global_blocked_services").toBoolean()),
                use_global_settings: (device.currentValue("use_global_settings").toBoolean()),
                tags: (convertMapString(device.currentValue("clientTags"))),
                ids: (convertMapString(device.currentValue("clientIds"))),
                blocked_services: (convertMapString(device.currentValue("blockedServices")))
            ]
        ]
    return attribMap
}


// State Collector
def getStatus() {
    logDebug "Getting Client Status...."
    List<String> currentIds = convertMapString(device.currentValue("clientIds"))
    def respValuesMap = parent.doHttpGet("/clients/find?ip0=${currentIds[0]}", null)
    def mapID = currentIds[0].toString()
    def respValues = respValuesMap["$mapID"][0]
    logDebug "Client Response: ${respValues}"
    sendEvent(name: "name", value: respValues.name)
    sendEvent(name: "use_global_settings", value: respValues.use_global_settings)
    sendEvent(name: "use_global_blocked_services", value: respValues.use_global_blocked_services)
    sendEvent(name: "filtering", value: respValues.filtering_enabled)
    sendEvent(name: "parental", value: respValues.parental_enabled)
    sendEvent(name: "safeBrowsing", value: respValues.safebrowsing_enabled)
    sendEvent(name: "safeSearch", value: respValues.safesearch_enabled)
    respValues.blocked_services.remove("null")
    sendEvent(name: "blockedServices", value: respValues.blocked_services)
    sendEvent(name: "clientIds", value: respValues.ids)
    sendEvent(name: "clientTags", value: respValues.tags)
}


// State Controls
def GlobalSettingsOn() {
    logDebug "Enabling Gloabl Settings for client...."
    clientAttribs = formatMap()
    clientAttribs.data.use_global_settings = true
    updateClient(clientAttribs)
}

def GlobalSettingsOff() {
    logDebug "Disabling Gloabl Settings for client...."
    clientAttribs = formatMap()
    clientAttribs.data.use_global_settings = false
    updateClient(clientAttribs)
}

def FilteringOn() {
    logDebug "Enabling Filtering for client...."
    clientAttribs = formatMap()
    clientAttribs.data.filtering_enabled = true
    updateClient(clientAttribs)
}

def FilteringOff() {
    logDebug "Disabling Filtering for client...."
    clientAttribs = formatMap()
    clientAttribs.data.filtering_enabled = false
    updateClient(clientAttribs)
}

def ParentalControlOn() {
    logDebug "Enabling Parental Control for client...."
    clientAttribs = formatMap()
    clientAttribs.data.parental_enabled = true
    updateClient(clientAttribs)
}

def ParentalControlOff() {
    logDebug "Disabling Parental Control for client...."
    clientAttribs = formatMap()
    clientAttribs.data.parental_enabled = false
    updateClient(clientAttribs)
}

def SafeBrowsingOn() {
    logDebug "Enabling SafeBrowsing for client...."
    clientAttribs = formatMap()
    clientAttribs.data.safebrowsing_enabled = true
    updateClient(clientAttribs)
}

def SafeBrowsingOff() {
    logDebug "Disabling SafeBrowsing for client...."
    clientAttribs = formatMap()
    clientAttribs.data.safebrowsing_enabled = false
    updateClient(clientAttribs)
}

def SafeSearchOn() {
    logDebug "Enabling SafeSearch for client...."
    clientAttribs = formatMap()
    clientAttribs.data.safesearch_enabled = true
    updateClient(clientAttribs)
}

def SafeSearchOff() {
    logDebug "Disabling SafeSearch for client...."
    clientAttribs = formatMap()
    clientAttribs.data.safesearch_enabled = false
    updateClient(clientAttribs)
}

def GlobalBlockListOn() {
    logDebug "Enabling Gloabl Block List for client...."
    clientAttribs = formatMap()
    clientAttribs.data.use_global_blocked_services = true
    updateClient(clientAttribs)
}

def GlobalBlockListOff() {
    logDebug "Disabling Gloabl Block List for client...."
    clientAttribs = formatMap()
    clientAttribs.data.use_global_blocked_services = false
    updateClient(clientAttribs)
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

        clientAttribs = formatMap()
        clientAttribs.data.blocked_services = postBlockList
        updateClient(clientAttribs)
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
        logDebug "Setting ${postBlockList.size} Blocked Services: ${postBlockList}"
        postBlockList.remove("null")

        clientAttribs = formatMap()
        clientAttribs.data.blocked_services = postBlockList
        updateClient(clientAttribs)
    } else {
        logError "No new services to unblock [${services_list.toString}]"
    }
}


// General Functions
def parentEvent(attrib_name, attrib_value) {
    logTrace "Updating Attribute ${attrib_name}: ${attrib_value}"
    sendEvent(name: attrib_name, value: attrib_value)
}

def updateClient(postMap) {
    logTrace "Sending client update...."
    parent.doHttpPostJson("/clients/update", postMap)
    pauseExecution(1000)
    getStatus()
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
