// Generates server-side metadata for Ant & Maven
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import hudson.util.VersionNumber
import net.sf.json.JSONObject

import java.util.regex.Pattern

def getHtmlPage(url) {
    def wc = new WebClient(BrowserVersion.FIREFOX_10, "proxy", 3128)
//    def wc = new WebClient()
//    wc.javaScriptEnabled = false;
//    wc.cssEnabled = false;
    return wc.getPage(url)
}

def listFromURL(url) {
    HtmlPage p = getHtmlPage(url)
    pattern = Pattern.compile("maven-([0-9.]+)(-bin)?.zip\$")

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute)
            return ["id":ver, "name":ver, "url":url.toExternalForm()]
        }
        return null;
    }
}

def listFromOldURL() {
    return listFromURL("http://archive.apache.org/dist/maven/binaries/")
}

def listFromNewUrl() {
    HtmlPage p = getHtmlPage("http://archive.apache.org/dist/maven/")
    pattern = Pattern.compile("maven-([0-9])/\$")

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        if(m.matches()) {
            url = p.getFullyQualifiedUrl(a.hrefAttribute)
            println url
            HtmlPage p1 = getHtmlPage(url)
            p1.getAnchors().collect { HtmlAnchor a1 ->
                url1 = p1.getFullyQualifiedUrl(a1.hrefAttribute)
                HtmlPage p2 = getHtmlPage(url1)
                p2.getAnchors().collect { HtmlAnchor a2 ->
//                    pattern2 = Pattern.compile("maven-([0-9])/([0-9\\.])+/binaries/\$")
                    pattern2 = Pattern.compile("binaries/\$")
                    url2 = p2.getFullyQualifiedUrl(a2.hrefAttribute)
//                    println url2.toExternalForm()
                    m2 = pattern2.matcher(url2.toExternalForm())
                    if(m2.matches()) {
                        println url2
//                        listFromURL(url2)
                    }
                }
            }
        }
    }.flatten()
}

def listAll() {
    return (listFromOldURL() + listFromNewUrl())
            .findAll { it!=null }.unique().sort { o1,o2 ->
        try {
            def v1 = new VersionNumber(o1.id)
            try {
                return new VersionNumber(o2.id).compareTo(v1)
            } catch (IllegalArgumentException _2) {
                return -1
            }
        } catch (IllegalArgumentException _1) {
            try {
                new VersionNumber(o2.id)
                return 1
            } catch (IllegalArgumentException _2) {
                return o2.id.compareTo(o1.id)
            }
        }
    }
}

def store(key,o) {
    JSONObject envelope = JSONObject.fromObject(["list": o])
    lib.DataWriter.write(key,envelope)
}

store("hudson.tasks.Maven.MavenInstaller", listAll())
