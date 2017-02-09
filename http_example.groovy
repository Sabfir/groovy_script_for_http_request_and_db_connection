//import com.google.api.client.http.HttpRequest
//import com.google.api.client.http.HttpResponse
//import com.google.api.client.http.json.JsonHttpContent
//import com.google.api.client.json.jackson2.JacksonFactory
//import groovyx.net.http.HTTPBuilder

/*
 * Copyright (c) 2012 Spillman Technologies, Inc.
 * All rights reserved.
 */
//@Grab('mysql:mysql-connector-java:5.1.25')
//@Grab('groovy.sql.Sql')
//@GrabConfig(systemClassLoader = true)
@Grab('mysql:mysql-connector-java:5.1.6')
@GrabConfig(systemClassLoader=true)
import groovy.sql.Sql

class SignUpTest {
    static String CONFIRM_PATH = "/signup/confirm/"
    static String SIGNUP_PATH = "/signup"

    private String baseUrl = "http://localhost:8080/api/v1"
    private String dbUrl = "jdbc:mysql://localhost:3306/citadex_client"
    private String dbUser = "root"
    private String dbPassword = "window"
    private String email = "test@test.com"
    private String password = "mypass"
    private String emailLoginPart
    private String emailDomainPart
    private String defaultFileName = '/credentials.txt'
    private String filePath = System.getProperty('user.dir') + defaultFileName
    private Integer attemptsCount

    SignUpTest(Integer attemptsCount, String filePath) {
        print "Use default settings (Y/N)? "
        def useDefault = System.in.newReader().readLine()
        if (!useDefault.equalsIgnoreCase("y")) {
            print "Enter host/port/baseUrl. Leave empty for default ($baseUrl):"
            def baseUrlNew = System.in.newReader().readLine()
            if (!baseUrlNew.isEmpty()) {
                baseUrl = baseUrlNew
            }
            print "Enter dbUrl. Leave empty for default ($dbUrl):"
            def dbUrlNew = System.in.newReader().readLine()
            if (!dbUrlNew.isEmpty()) {
                dbUrl = dbUrlNew
            }
            print "Enter dbUser. Leave empty for default ($dbUser):"
            def dbUserNew = System.in.newReader().readLine()
            if (!dbUserNew.isEmpty()) {
                dbUser = dbUserNew
            }
            print "Enter dbPassword. Leave empty for default ({$dbPassword}):"
            def dbPasswordNew = System.in.newReader().readLine()
            if (!dbPasswordNew.isEmpty()) {
                dbPassword = dbPasswordNew
            }
            print "Enter email for user profile. Leave empty for default ({$email}):"
            def emailNew = System.in.newReader().readLine()
            if (!emailNew.isEmpty()) {
                email = emailNew
            }
        }

        def emailArr = email.split('@')
        this.emailLoginPart = emailArr[0]
        this.emailDomainPart = '@' + emailArr[1]
        this.attemptsCount = attemptsCount
        if (!filePath.isEmpty()) {
            this.filePath = filePath
        }

        printSettings()
    }

    private void printSettings() {
        println "-----WORKED OUT WITH SETTINGS:-----"
        println "host/port/baseUrl: $baseUrl"
        println "dbUrl: $dbUrl"
        println "dbUser: $dbUser; dbPassword: $dbPassword"
        println "-----------------------------------"
    }

    void testSignUpWithConfirm() {
        println("users to be created ${this.attemptsCount}")
        println("creating file ${this.filePath}")
        def fileCredentials = new File(filePath)
        cleanFile(fileCredentials)
        for (int i= 0; i < attemptsCount; i++) {
            this.email = generateEmail(i)

            try {
                println "-------------SIGNUP & CONFIRM START ($email)--------------"
                doSignUp()
                doConfirm()
                fileCredentials << 'email: ' + this.email + '; password: ' + this.password + '\n'
            } catch (Exception e) {
                println("ERROR!!!" + e.getMessage())
            }
        }
    }

    private void doSignUp() {
        println "sign up start"
        def address = baseUrl + SIGNUP_PATH
        def data = toJSON(getProfile())
        data = "{" + data + "}"
        def urlInfo = address.toURL()

        def connection = urlInfo.openConnection()
        connection.setRequestMethod("POST")
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json")
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(data);
        writer.flush();
        println "response message {$connection.responseMessage}"
        println "response content {$connection.content}"
        writer.close();
    }

    private boolean doConfirm() {
        println "confirm start"
        def sql=Sql.newInstance(dbUrl, dbUser, dbPassword, "com.mysql.jdbc.Driver")
        def rows = sql.rows ("SELECT confirmation FROM iris_user WHERE user_id =(SELECT id FROM user_view where email = $email)")
        def confirmation = rows[0].confirmation
        def address = baseUrl + CONFIRM_PATH + confirmation
        println "address: {$address}"
        def urlInfo = address.toURL()

        def connection = urlInfo.openConnection()
        connection.setRequestMethod("POST")
        println "response message {$connection.responseMessage}"
    }

    private String generateEmail(Integer counter){
        return emailLoginPart + counter + emailDomainPart
    }

    Map getProfile() {
        Map profile =
                [
                        "personal" : [
                                "firstName"          : "FirstName",
                                "middleName"         : "Sr",
                                "lastName"           : "LastName",
                                "gender"             : "female",
                                "birthDate"          : "1989-11-04T00:00:00Z",
                                "email"              : this.email,
                                "mobilePhone"        : "0123456789",
                                "driverLicenseNumber": "12ed",
                                "driverLicenseState" : "Arizona"
                        ],
                        "addresses": [
                                "home": [
                                        "streetAddress"  : "homeStreet",
                                        "city"           : "homeCity",
                                        "state"          : "Alaska",
                                        "zipCode"        : "12345",
                                        "subAddress"     : "46",
                                        "phone"          : "1123456789"
                                ]
                        ],
                        "agencies" : [
                                "defaultAgency": [
                                        "id"  : 2,
                                        "name": "Kenmare Police Department"
                                ]
                        ],
                        "password" : [
                                "password"            : this.password,
                                "passwordConfirmation": this.password
                        ]
                ]
        return profile;
    }

    private void cleanFile(File file) {
        PrintWriter pw = new PrintWriter(file);
        pw.close();
    }

    private def toJSON(elements, depth = 0, recursively = false) {
        def json = ""
        depth.times { json += "\t" }
        if (recursively) {
            json += "{"
        }
        elements.each { key, value ->
            json += "\"$key\":"
            json += jsonValue(value, depth)
            json += ", "
        }

        json = (elements.size() > 0) ? json.substring(0, json.length() - 2) : json
        if (recursively) {
            json += "}"
        }
        json
    }

    private def jsonValue(element, depth) {
        if (element instanceof Map) {
            return "\n" + toJSON(element, depth + 1, true)
        }
        if (element instanceof List) {
            def list = "["
            element.each { elementFromList ->
                list += jsonValue(elementFromList, depth)
                list += ", "
            }
            list = (element.size() > 0) ? list.substring(0, list.length() - 2) : list
            list += "]"
            return list
        }
        (element instanceof String) ? "\"$element\"": element?.toString()
    }
}

println("SIGNUP & CONFIRM TEST")

def attemptsCount = 0;
def filePath = ''
if (args.length) {
    attemptsCount = args[0].toInteger()
}
if (args.length > 1) {
    filePath = args[1]
}
SignUpTest testSuite = new SignUpTest(attemptsCount, filePath)

testSuite.testSignUpWithConfirm()

println "DONE!"


//String getProfileAsString() {
//    String profile = "\t\"personal\":\n" +
//            "\t{\n" +
//            "\t\t\"firstName\":\"FirstName\", \"middleName\":\"Sr\", \"lastName\":\"LastName\", \"gender\":\"female\", " +
//            "\"birthDate\":\"1989-11-04T00:00:00Z\", \"email\":\"email1@test.com\", \"mobilePhone\":\"0123456789\", \"driverLicenseNumber\":\"12ed\", " +
//            "\"driverLicenseState\":\"Arizona\"\n" +
//            "\t},\n" +
//            "\t\"addresses\":\n" +
//            "\t{\n" +
//            "\t\t\"home\":\n" +
//            "\t\t{\n" +
//            "\t\t\t\"streetAddress\":\"homeStreet\", \"city\":\"homeCity\", \"state\":\"Alaska\", \"zipCode\":\"12345\", \"subAddress\":\"46\", \"phone\":\"1123456789\"\n" +
//            "\t\t}\n" +
//            "\t},\n" +
//            "\t\"agencies\":\n" +
//            "\t{\n" +
//            "\t\t\"defaultAgency\":\n" +
//            "\t\t{\n" +
//            "\t\t\t\"id\":\"2\", \"name\":\"Kenmare Police Department\"\n" +
//            "\t\t}\n" +
//            "\t},\n" +
//            "\t\"password\":\n" +
//            "\t{\n" +
//            "\t\t\"password\":\"mypass\", \"passwordConfirmation\":\"mypass\"\n" +
//            "\t}\n" +
//            ""
//    return profile
//}

//private httpGetExample() {
//    def address = "http://localhost:8080/anonymous/agencies"
//    def urlInfo = address.toURL()
//    println "URL: ${address}"
//    println "Host/Port: ${urlInfo.host}/${urlInfo.port}"
//    println "Protocol: ${urlInfo.protocol}"
//
//    def connection = urlInfo.openConnection()
//    println "Connection Type: ${connection.class}"
//    println "Content Type: ${connection.contentType}"
//    println "Response Code/Message: ${connection.responseCode} / ${connection.responseMessage}"
//    println "Request Method: ${connection.requestMethod}"
//    println "Date: ${connection.date}"
//    println "Last-Modified: ${connection.lastModified}"
//    println "Content Length: ${connection.contentLength}"
//    def headerFields = connection.getHeaderFields()
//    headerFields.each {println "header fields: {$it}"}
//    println "response message {$connection.responseMessage}"
//    println "-------------CONTENT--------------"
//    def content = connection.getContent()
//    println "response message {$content}"
//}

//private httpPostExample() {
//    def address = "http://localhost:8080/api/v1/signup"
//    def data = toJSON(getProfile())
//    data = "{" + data + "}"
//    println "data: {$data}"
//    def urlInfo = address.toURL()
//    println "URL: ${address}"
//    println "Host/Port: ${urlInfo.host}/${urlInfo.port}"
//    println "Protocol: ${urlInfo.protocol}"
//
//    def connection = urlInfo.openConnection()
//    connection.setRequestMethod("POST")
//    connection.setDoOutput(true);
//    connection.setRequestProperty("Content-Type", "application/json")
//    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
//    writer.write(data);
//    writer.flush();
//    println "Connection Type: ${connection.class}"
//    println "Content Type: ${connection.contentType}"
//    println "Response Code/Message: ${connection.responseCode} / ${connection.responseMessage}"
//    println "Request Method: ${connection.requestMethod}"
//    println "Date: ${connection.date}"
//    println "Last-Modified: ${connection.lastModified}"
//    println "Content Length: ${connection.contentLength}"
//    def headerFields = connection.getHeaderFields()
//    headerFields.each {println "header fields: {$it}"}
//    println "response message {$connection.responseMessage}"
//    println "-------------CONTENT--------------"
//    def content = connection.getContent()
//    println "response message {$content}"
//    writer.close();
//}