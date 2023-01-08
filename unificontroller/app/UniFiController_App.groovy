/**
 *  Unifi Controller Manager for Hubitat
 *  Author: JoKneeMo <https://github.com/JoKneeMo>
 *  License: GPL-3.0-only
 *  Version: 1.0.0
*/

import groovyx.net.http.ContentType
import groovy.transform.Field

definition(
    name: "UniFi Controller Manager",
    namespace: "ubiquiti-unifi",
    author: "JoKneeMo",
    description: "Manages UniFi Controller instances and serves Protect camera snapshot images for the Hubitat dashboard",
    category: "Dashboard",
    iconUrl: "",
    iconX2Url: "",
    oauth: [displayName: "UniFi Protect Snapshots", displayLink: "https://ui.com"],
    singleInstance: true
){}

preferences {
    page(name: "mainPage")
    page(name: "newController")
    page(name: "addController")
    page(name: "configureController")
    page(name: "createControllerChildren")
    page(name: "deletePDevice")
    page(name: "changeName")
    page(name: "dashboardHelp")
}

def mainPage() {
    def oauthEnabled = isOAuthEnabled()

    if (tokenReset) {
        app.updateSetting("tokenReset", false)
        state.accessToken = null
        createAccessToken()
    }

    dynamicPage(name: "mainPage", title: "<h2>Manage Your UniFi Controllers</h2>", nextPage: null, uninstall: true, install: true) {
        section("<h3>Installed Controllers</h3>") {
            getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
                href "configureController", title: "$it.label", description: "", params: [did: it.deviceNetworkId]
            }
            href "newController", title: "New UniFi Controller", description: ""
        }

        section("<h3>UniFi Protect Camera Snapshots</h3>"){
            if (!oauthEnabled) {
                paragraph('<b style="color: red;">OATH is not currently enabled under the "OAuth" button in the app code editor for this app.\n'
                    + 'This is required if you wish to embed images in your dashboards.</b>')
            }
            href "dashboardHelp", title: "View Dashboard Snapshot Configuration", description: ""
        }
    }
}

def newController() {
    dynamicPage(name: "newController", title: "<h2>New UniFi Controller</h2>", nextPage: "mainPage", uninstall: false) {
        section {
            paragraph("Complete the fields below, then press the \"Add Controller\" button.\n"
                + "If your controller run on UniFi OS, only the hostname or IP address should be entered.\n"
                + "If you're using a classic controller, be sure to include the port number in the IP field."
            )

            input name: "controllerName", type: "text", title: "New Controller Device Name", required: true, defaultValue: "UniFi Controller"
            input name: "controllerIP", type: "text", title: "UniFi Controller IP", required: true, defaultValue: "192.168.1.1"
            input name: "username", type: "text", title: "Username", required: true
            input name: "password", type: "password", title: "Password", required: true
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
            
            href "addController", title: "Add Controller", description: "Complete the info above and click here to add it"
        }
    }
}

def addController() {
    def formattedDNI = UNIFI_API_DNI + "||" + UUID.randomUUID().toString()
    def driver = DEVICE_TYPES[UNIFI_API_DNI].driver

    try {
        d = createDevice(driver, formattedDNI, controllerName.toString())

        d.updateSetting("controllerIP", [value: controllerIP.toString(), type: "text"])
        d.updateSetting("username", [value: username.toString(), type: "text"])
        d.updateSetting("password", [value: password.toString(), type: "password"])
        d.updateSetting("logEnable", [value: logEnable.toString(), type: "bool"])

        d.initialize()
        d.refresh()

        logInfo "${DEVICE_TYPES[UNIFI_API_DNI].name} with ID ${formattedDNI} created..."

        dynamicPage(name: "addController", title: "<h2>Add Controller Summary</h2>", nextPage: "mainPage") {
            section {
                paragraph("The device has been created. Press \"Next\" to continue")
            }
        }
    }
    catch(e) {
        dynamicPage(name: "addController", title: "<h2>Add Controller Summary</h2>", nextPage: "mainPage") {
            section {
                paragraph("Error: ${(e as String).split(": ")[1]}.")
                paragraph("The device could not be created. Press \"Next\" to continue")
            }
        }
    }
}

def dashboardHelp() {
    def oauthEnabled = isOAuthEnabled()

    if (tokenReset) {
        app.updateSetting("tokenReset", false)
        state.accessToken = null
        createAccessToken()
    }

    dynamicPage(name: "dashboardHelp", title: '<h2>Snapshots in Dashboards</h2>', uninstall: false, nextPage: "mainPage") {
        if (!oauthEnabled) {
            paragraph('<b style="color: red;">OATH is not currently enabled under the "OAuth" button in the app code editor for this app.  This is required if you wish to embed images in your dashboards.</b>')
        }

        section('<h3>Steps to include snapshots on a dashboard</h3>') {
            paragraph("These steps must be performed for each of the cameras to be included.")
            paragraph("- In the dashboard that will show the thumbnail image click the \"+\" button to add a new tile.\n"
                + "- Choose a placement for the tile on the dashboard with the column, row, height and width arrow controls.\n"
                + "- No device is necessary so DO NOT pick one in the \"Pick A Device\" list.  Instead pick \"Image\" from the template list.\n"
                + "- At the bottom of this page are listed all the available camera URLs.  Copy and paste the desired camera's URL into the \"Background Image Link\" field or \"Image URL\".  If you use the \"Background Image Link\" the image will fill the entire tile.  If you use \"Image URL\" the tile will display letter boxes.\n"
                + "- Only place the url in one of the two fields, not both.\n"
                + "- Enter a \"Refresh Interval (seconds)\""
                + "- Click the \"Add Tile\" button."
            )
        }

        section('<h3>Snapshots and URLs</h3>') {
            def imagePath = "<b>../../<code>${app.id}</code>/snapshot/<code>[Controller ID]</code>/<code>[Device ID]</code>?access_token=<code>${atomicState.accessToken}</code></b>"
            paragraph(imagePath)
            paragraph("The URL breaks down into the following pieces:\n"
                + "- \"../../\" -- This instructs the web page to go two levels back when accessing the app. This is what makes the snapshots work on local and cloud dashboards.\n"
                + "- \"${app.id}\" -- The app instance ID of the \"UniFi Controller Manager\" app. This can also be seen in the URL if you go to the app from the \"Apps\" link in the left navigation pane.\n"
                + "- [Controller ID] - The DNI (device network ID) of the UniFi Controller that has the camera. Do not include the square brackets.\n"
                + "- [Device ID] - The DNI (device network ID) of the camera from which to pull the snapshot. Do not include the square brackets.\n"
                + "- The last GUID is the access token created by this app using OAuth that requests will use to authenticate to Hubitat. <b>DO NOT</b> disclose this to anybody.  It is like a password.\n"
            )
            paragraph("If the URL above is blank or incomplete then you must enable OAuth for this app under \"Apps Code\" in Hubitat where this app was installed.")
        }


        section('<h3>Snapshots</h3>') {
            if (getControllerChildren(state.currentDeviceId).findAll { it.typeName == DEVICE_TYPES["camera"].driver }.size() == 0) {
                paragraph("<b>There are no camera devices on this UniFi controller.</b>")
            }
            else {
                paragraph(
                    getControllerChildren(state.currentDeviceId).findAll{ it.typeName == DEVICE_TYPES["camera"].driver }.collect {
                        parentID = it.getParent().deviceNetworkId.replace("UNIFI_API_DNI||", "")
                        def url = "${getLocalApiServerUrl()}/${app.id}/snapshot/${parentID}/${it.deviceNetworkId}?access_token=${atomicState.accessToken}"
                        def cloudURL = "${getApiServerUrl()}/snapshot/${parentID}/${it.deviceNetworkId}?access_token=${atomicState.accessToken}"
                        "<u><b>${it.label}:</b></u>\n" +
                        "<b>Local URL</b>: ${url}\n" +
                        "<b>Cloud URL</b>: ${cloudURL}\n" +
                        "<b>Dashboard URL</b>: ../../${app.id}/snapshot/${parentID}/${it.deviceNetworkId}?access_token=${atomicState.accessToken}\n" +
                        "<b>Current Snapshot</b>:\n" +
                            "<img height=\"180\" width=\"320\" src=\"${url}\" alt=\"Snapshot\" />"
                    }.join("\n\n\n")
                )
            }
        }

        section('<h3>Resetting the OAuth Access Token</h3>') {
            paragraph("<b style=\"color: red;\">Do not toggle this button without understanding the following.</b>  Resetting this token will require you to update all of the URLs in any existing dashboard tile <b>AS WELL AS</b> any URL in any IFTTT applet you have configured.  There is no need to reset the token unless it was compromised.")
            preferences {
                input name: "tokenReset", type: "bool", title: "Toggle this to reset your app's OAuth token", defaultValue: false, submitOnChange: true
            }
        }
    }
}


def configureController(params) {
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
    if (getChildDevice(state.currentDeviceId) != null) getChildDevice(state.currentDeviceId).configure()
    dynamicPage(name: "configureController", title: "<h2>Configure Existing UniFi Controllers</h2>", nextPage: "mainPage") {
        if (state.currentDeviceId.startsWith(UNIFI_API_DNI)) {
            section {
                paragraph("This is the virtual device that holds the connection for your UniFi Controller.")
                paragraph("The devices attached to the controller are listed in the \"Child Devices\" section below.\n"
                    + "If no devices are listed, you can create them by pressing the \"Create Child Devices\" button.\n"
                    + "If there is a child device that you don't want, simply delete it after you finish creating all devices.")
            }
        }
        
        section("<h3>Rename Controller Device</h3>"){
            app.updateSetting("${state.currentDeviceId}_label", getChildDevice(state.currentDeviceId).label)
            input "${state.currentDeviceId}_label", "text", title: "Device Name", description: "", required: false
            href "changeName", title: "Change Device Name", description: "Edit the name above and click here to change it"
        }

        section("<h3>Controller Details</h3>") {
            paragraph("<strong>Connection Status: </strong>" + getChildDevice(state.currentDeviceId).currentState("commStatus").value + "\n"
                + "<strong>IP Address: </strong>" + getChildDevice(state.currentDeviceId).getSetting("controllerIP").value + "\n"
                + "<strong>Username: </strong>" + getChildDevice(state.currentDeviceId).getSetting("username").value + "\n"
                + "<strong>Debug Logging: </strong>" + getChildDevice(state.currentDeviceId).getSetting("logEnable").value + "\n"
                + "<strong>Last Activity At: </strong>" + getChildDevice(state.currentDeviceId).getLastActivity().toString()
            )
        }

        section("<h3>Child Devices</h3>"){
            if (getControllerChildren(state.currentDeviceId) != null) {
                paragraph("<ul>\n" +
                    getControllerChildren(state.currentDeviceId).sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).collect {
                        def url = "${getLocalApiServerUrl().replace("/apps/api", "")}/device/edit/${it.id}"
                        "<li><a href=\"${url}\" target=\"_blank\">${it.label}</a></li>"
                    }.join("\n") + 
                    "</ul>"
                )
                href "createControllerChildren", title: "Refresh Child Devices", description: ""
            }
            else {
                paragraph("Controller has no children")
                href "createControllerChildren", title: "Create Child Devices", description: ""
            }
            
        }

        section("<h3>Delete Controller Device</h3>"){
            paragraph('<b style="color: red;">WARNING!!  ADVERTENCIA!!  ACHTUNG!!  AVERTISSEMENT!!</b>')
            paragraph("There is not a confirmation for these delete buttons!")
            href "deletePDevice", title: "Delete $state.currentDisplayName", description: ""
        }
    }
}

def deletePDevice() {
    try {
        unsubscribe()
        deleteChildDevice(state.currentDeviceId)
        dynamicPage(name: "deletePDevice", title: "<h2>UniFi Controler Deletion Result</h2>", nextPage: "mainPage") {
            section {
                paragraph("The device has been deleted. Press \"Next\" to continue.")
            }
        }
    }
    catch (e) {
        dynamicPage(name: "deletePDevice", title: "<h2>UniFi Controler Deletion Result</h2>", nextPage: "mainPage") {
            section {
                paragraph("Error: ${(e as String).split(": ")[1]}.")
            }
        }
    }
}

def changeName() {
    def thisDevice = getChildDevice(state.currentDeviceId)
    thisDevice.label = settings["${state.currentDeviceId}_label"]

    dynamicPage(name: "changeName", title: "<h2>UniFi Controller Change Name Result</h2>", nextPage: "mainPage") {
        section {
            paragraph("The device has been renamed. Press \"Next\" to continue")
        }
    }
}

def createControllerChildren() {
    def thisDevice = getChildDevice(state.currentDeviceId)
    def connectionStatus = thisDevice.currentState("commStatus").value

    if ("${connectionStatus}" == "good") {
        try {
            thisDevice.createChildDevices()
            dynamicPage(name: "createControllerChildren", title: "<h2>Create Child Devices</h2>", nextPage: "configureController") {
                section {
                    paragraph("The child devices have been created. Press \"Next\" to continue")
                }
            }
        }
        catch (e) {
            dynamicPage(name: "createControllerChildren", title: "<h2>Failed to Create Child Devices</h2>", nextPage: "configureController") {
                section {
                    paragraph("The child devices could not be created.")
                    paragraph("Error: ${(e as String).split(": ")[1]}.")
                }
            }
        }
        
    }
    else {
        dynamicPage(name: "createControllerChildren", title: "<h2>UniFi Controller is not connected</h2>", nextPage: "configureController") {
            section {
                paragraph("The controller is not connected. Correct the connection between Hubitat and the UniFi controller, then try again.")
            }
        }
    }
}

def serveSnapshot() {
    def controller = getChildDevice("UNIFI_API_DNI||" + params.controllerId)
    logDebug "serveSnapshot(controller: UNIFI_API_DNI||${params.controllerId}, device: ${params.ufpDeviceId})"
    logTrace "params: $params"
    
    def d = controller.getChildDevice(params.ufpDeviceId)
    if (d == null) {
        log.error "Could not locate a device with an id of ${params.ufpDeviceId}"
        return ["error": true, "type": "invalid value", "message": "Device Not Found"]
    }
    else {
        try {
            d.take()
            byte[] img = hubitat.helper.HexUtils.hexStringToByteArray(d.currentState("image").value)
            snapWidth = d.getSetting("snapWidth").value
            snapHeight = d.getSetting("snapHeight").value
            imageResponse(img, snapWidth, snapHeight)
        }
        catch(e) {
            byte[] img
            imageResponse(img, "640", "360")
        }
    }
}

def imageResponse(byte[] img, width, height) {
    String strImg
    if (!img || img.length <= 2) {
        logTrace "Default to missing image"
        strImg = MISSING_IMG
    }
    else {
        strImg = "data:image/jpeg;base64,${img.encodeBase64().toString()}"
    }

    render contentType: "image/svg+xml", data: "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
        "height=\"${height}\" width=\"${width}\"><image width=\"${width}\" height=\"${height}\" xlink:href=\"${strImg}\"/></svg>", status: 200
}


mappings { 
    path("/snapshot/:controllerId/:ufpDeviceId") {
        action: [GET: "serveSnapshot"]
    }
}


// General app functions

def String getFormattedDNI(id) {
    return "UniFi-${id}"
}

def getControllerChildren(controller) {
    def thisDevice = getChildDevice(controller)
    def children = thisDevice.getChildDevices()

    return children
}

def isOAuthEnabled() {
    def oauthEnabled = true
    try {
        if (!state.accessToken) {
        createAccessToken()
        }
    }
    catch (ex) {
        oauthEnabled = false
    }
    return oauthEnabled
}

def createDevice(driver, id, label) {
    return addChildDevice("tomw", driver, id, null, ["label": label])
}

//logging help methods
private logInfo(msg) {
    if (descriptionTextEnable) log.info msg
}

def logDebug(msg) {
    if (logEnable) log.debug msg
}

def logTrace(msg) {
    if (traceLogEnable) log.trace msg
}


// Constants
@Field static def UNIFI_API_DNI = "UNIFI_API_DNI"
@Field static def GET = "httpGet"

@Field static String MISSING_IMG = "data:image/png;base64,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"

@Field static def DEVICE_TYPES = [
    "UNIFI_API_DNI": [name: "UniFi Protect Controller", driver: "UniFi Protect Controller"],
    "camera": [name: "UniFi Protect Camera", driver: "UniFi Protect Camera"],
    "doorbell": [name: "UniFi Protect Doorbell", driver: "UniFi Protect Doorbell"],
    "light": [name: "UniFi Protect Light", driver: "UniFi Protect Light"]
]