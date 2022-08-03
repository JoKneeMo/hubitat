/**
 *  AdGuard Home DNS Server Manager for Hubitat
 *  Author: JoKneeMo <https://github.com/JoKneeMo>
 *  Copyright: JoKneeMo <https://github.com/JoKneeMo>
 *  License: GPL-3.0-only
 *  Version: 1.0.0
*/

import groovyx.net.http.ContentType
import groovy.transform.Field

definition(
    name: "AdGuard Home Manager",
    namespace: "adguard-home",
    author: "JoKneeMo",
    description: "Manages AdGuard Home servers and allows client control",
    category: "Integrations",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true
){}

preferences {
    page(name: "mainPage")
    page(name: "newServer")
    page(name: "addServer")
    page(name: "configureServer")
    page(name: "createServerChildren")
    page(name: "deleteServer")
    page(name: "changeName")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "<h2>Manage Your AdGuard Home DNS Servers</h2>", nextPage: null, uninstall: true, install: true) {
        section("<h3>Managed Servers</h3>") {
            getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
                href "configureServer", title: "$it.label", description: "", params: [did: it.deviceNetworkId]
            }
            href "newServer", title: "New DNS Server", description: ""
        }
    }
}

def newServer() {
    dynamicPage(name: "newServer", title: "<h2>New AdGuard Home DNS Server</h2>", nextPage: "mainPage", uninstall: false) {
        section {
            paragraph("Complete the fields below, then press the \"Add Server\" button.\n")

            input name: "serverName", type: "text", title: "New Server Name", required: true, defaultValue: "AdGuard Home DNS Server"
            input name: "serverIP", type: "text", title: "Server IP Address", required: true, defaultValue: "192.168.1.2"
            input name: "username", type: "text", title: "Username", required: true
            input name: "password", type: "password", title: "Password", required: true
            input name: "tlsEnable", type: "bool", title: "TLS Connection", defaultValue: false
            input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: true
            
            href "addServer", title: "Add Server", description: "Complete the info above and click here to add it"
        }
    }
}

def addServer() {
    def formattedDNI = AGH_API_DNI + "||" + UUID.randomUUID().toString()
    def driver = DEVICE_TYPES[AGH_API_DNI].driver

    try {
        d = createDevice(driver, formattedDNI, serverName.toString())

        d.updateSetting("serverIP", [value: serverIP.toString(), type: "text"])
        d.updateSetting("username", [value: username.toString(), type: "text"])
        d.updateSetting("password", [value: password.toString(), type: "password"])
        d.updateSetting("tlsEnable",[value: tlsEnable.toString(), type: "bool"])
        d.updateSetting("logDebug", [value: logDebug.toString(), type: "bool"])

        d.initialize()
        d.refresh()

        logInfo "${DEVICE_TYPES[AGH_API_DNI].name} with ID ${formattedDNI} created..."

        dynamicPage(name: "addServer", title: "<h2>Add Server Summary</h2>", nextPage: "mainPage") {
            section {
                paragraph("The device has been created. Press \"Next\" to continue")
            }
        }
    }
    catch(e) {
        dynamicPage(name: "addServer", title: "<h2>Add Server Summary</h2>", nextPage: "mainPage") {
            section {
                paragraph("Error: ${(e as String).split(": ")[1]}.")
                paragraph("The device could not be created. Press \"Next\" to continue")
            }
        }
    }
}



def configureServer(params) {
    if (params?.did || params?.params?.did) {
        if (params.did) {
            state.currentDeviceId = params.did
            state.currentDisplayName = getChildDevice(params.did)?.displayName
        }
        else {
            state.currentDeviceId = params.params.did
            state.currentDisplayName = getChildDevice(params.params.did)?.displayName
        }
    }
    if (getChildDevice(state.currentDeviceId) != null) getChildDevice(state.currentDeviceId).refresh()
    dynamicPage(name: "configureServer", title: "<h2>Configure Existing AdGuard Home DNS Servers</h2>", nextPage: "mainPage") {
        if (state.currentDeviceId.startsWith(AGH_API_DNI)) {
            section {
                paragraph("This is the virtual device that holds the connection for your DNS Server.")
                paragraph("The devices attached to the server are listed in the \"Child Devices\" section below.\n"
                    + "If no devices are listed, you can create them by pressing the \"Create Child Devices\" button.\n"
                    + "If there is a child device that you don't want, simply delete it after you finish creating all devices.")
            }
        }
        
        section("<h3>Rename DNS Server Device</h3>"){
            app.updateSetting("${state.currentDeviceId}_label", getChildDevice(state.currentDeviceId).label)
            input "${state.currentDeviceId}_label", "text", title: "Device Name", description: "", required: false
            href "changeName", title: "Change Device Name", description: "Edit the name above and click here to change it"
        }

        section("<h3>DNS Server Details</h3>") {
            paragraph("<strong>IP Address: </strong>" + getChildDevice(state.currentDeviceId).getSetting("serverIP").value + "\n"
                + "<strong>Username: </strong>" + getChildDevice(state.currentDeviceId).getSetting("username").value + "\n"
                + "<strong>Version: </strong>" + getChildDevice(state.currentDeviceId).currentValue("version").value + "\n"
                + "<strong>Protection: </strong>" + getChildDevice(state.currentDeviceId).currentValue("protection").value + "\n"
                + "<strong>Filtering: </strong>" + getChildDevice(state.currentDeviceId).currentValue("filtering").value + "\n"
                + "<strong>Parental Control: </strong>" + getChildDevice(state.currentDeviceId).currentValue("parental").value + "\n"
                + "<strong>Safe Browsing: </strong>" + getChildDevice(state.currentDeviceId).currentValue("safeBrowsing").value + "\n"
                + "<strong>Safe Search: </strong>" + getChildDevice(state.currentDeviceId).currentValue("safeSearch").value + "\n"
                + "<strong>DHCP Server: </strong>" + getChildDevice(state.currentDeviceId).currentValue("dhcp").value + "\n"
                + "<strong>Blocked Services: </strong>" + getChildDevice(state.currentDeviceId).currentValue("blockedServices").value + "\n"
                + "<strong>Debug Logging: </strong>" + getChildDevice(state.currentDeviceId).getSetting("logDebug").value + "\n"
                + "<strong>Trace Logging: </strong>" + getChildDevice(state.currentDeviceId).getSetting("logTrace").value + "\n"
                + "<strong>Last Activity At: </strong>" + getChildDevice(state.currentDeviceId).getLastActivity().toString()
            )
        }

        section("<h3>Child Devices</h3>"){
            if (getServerChildren(state.currentDeviceId) != null) {
                paragraph("<ul>\n" +
                    getServerChildren(state.currentDeviceId).sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).collect {
                        def url = "${getLocalApiServerUrl().replace("/apps/api", "")}/device/edit/${it.id}"
                        "<li><a href=\"${url}\" target=\"_blank\">${it.label}</a></li>"
                    }.join("\n") + 
                    "</ul>"
                )
                href "createServerChildren", title: "Refresh Child Devices", description: ""
            }
            else {
                paragraph("DNS Server has no children")
                href "createServerChildren", title: "Create Child Devices", description: ""
            }
            
        }

        section("<h3>Delete Server Device</h3>"){
            paragraph('<b style="color: red;">WARNING!!  ADVERTENCIA!!  ACHTUNG!!  AVERTISSEMENT!!</b>')
            paragraph("There is not a confirmation for these delete buttons!")
            href "deleteServer", title: "Delete $state.currentDisplayName", description: ""
        }
    }
}

def deleteServer() {
    try {
        unsubscribe()
        deleteChildDevice(state.currentDeviceId)
        dynamicPage(name: "deleteServer", title: "<h2>AdGuard Home DNS Server Deletion Result</h2>", nextPage: "mainPage") {
            section {
                paragraph("The device has been deleted. Press \"Next\" to continue.")
            }
        }
    }
    catch (e) {
        dynamicPage(name: "deleteServer", title: "<h2>AdGuard Home DNS Server Deletion Result</h2>", nextPage: "mainPage") {
            section {
                paragraph("Error: ${(e as String).split(": ")[1]}.")
            }
        }
    }
}

def changeName() {
    def thisDevice = getChildDevice(state.currentDeviceId)
    thisDevice.label = settings["${state.currentDeviceId}_label"]

    dynamicPage(name: "changeName", title: "<h2>AdGuard Home DNS Server Change Name Result</h2>", nextPage: "mainPage") {
        section {
            paragraph("The device has been renamed. Press \"Next\" to continue")
        }
    }
}

def createServerChildren() {
    def thisDevice = getChildDevice(state.currentDeviceId)
    try {
        thisDevice.createChildDevices()
        dynamicPage(name: "createServerChildren", title: "<h2>Create Child Devices</h2>", nextPage: "configureServer") {
            section {
                paragraph("The child devices have been created. Press \"Next\" to continue")
            }
        }
    }
    catch (e) {
        dynamicPage(name: "createServerChildren", title: "<h2>Failed to Create Child Devices</h2>", nextPage: "configureServer") {
            section {
                paragraph("The child devices could not be created.")
                paragraph("Error: ${(e as String).split(": ")[1]}.")
            }
        }
    }
}

// General app functions

def String getFormattedDNI(id) {
    return "AGH-${id}"
}

def getServerChildren(server) {
    def thisDevice = getChildDevice(server)
    def children = thisDevice.getChildDevices()

    return children
}

def createDevice(driver, id, label) {
    return addChildDevice("JoKneeMo", driver, id, null, ["label": label])
}

//logging help methods
private logInfo(msg) {
    if (descriptionTextEnable) log.info msg
}

def logDebug(msg) {
    if (logDebug) log.debug msg
}

def logTrace(msg) {
    if (traceLogEnable) log.trace msg
}


// Constants
@Field static def AGH_API_DNI = "AGH_API_DNI"
@Field static def GET = "httpGet"
@Field static def POST = "httpPost"

@Field static def DEVICE_TYPES = [
    "AGH_API_DNI": [name: "AdGuard Home DNS Server", driver: "AdGuard Home DNS Server"],
    "client": [name: "AdGuard Home DNS Client", driver: "AdGuard Home DNS Client"]
]