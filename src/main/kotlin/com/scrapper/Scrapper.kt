package com.scrapper
import com.beust.klaxon.Json
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.Klaxon
import com.gargoylesoftware.htmlunit.ScriptResult
import com.gargoylesoftware.htmlunit.javascript.host.dom.Text
import net.sourceforge.htmlunit.corejs.javascript.Script
import netscape.javascript.JSObject
import java.applet.Applet

data class Card(val url : String, val name : String, val fp : String, val cp : String)

class Scrapper(){
    private val client = WebClient(BrowserVersion.CHROME).apply{
        options.isCssEnabled = false
        options.isJavaScriptEnabled = true
        options.isUseInsecureSSL = true
    }

    /*
    * Potential implementations:
    * - parse data directly from game url
    *   - go in the url we parsed from the third party site
    *   - find out if game via the html
    *       - its obfuscated, hard to parse
    * - use some REST API
    *   - google doesnt have one but there are third party ones, problem is you have to pay
    *       - https://rapidapi.com/danielamitay/api/App%20Stores/functions
    *       - https://rapidapi.com/maxcanna/api/Google%20Play%20Store
    * - translate this third party API code
    *   - https://github.com/facundoolano/google-play-scraper/blob/dev/lib/app.js
    *
    * */


    fun scrap(url : String){
        println("Project Started")

        var currUrl = url
        var nextPageArgs = "init"
        var list = mutableListOf<Card>()
        try {
            //runs at least once since initially prevPage is 0 and currPage is 1, handles single page case
            while(nextPageArgs != ""){

                val page = client.getPage<HtmlPage>(currUrl)

                //get the next page url and setup the next currPage value

                //get the list of pages in the bottom of the html
                val nextPages = page.querySelectorAll("div.col.s12.section-buttons > ul.pagination > li")
                //get the last element which is the next page, if it is the last page pressing next will simply go to the same page
                nextPageArgs  = nextPages.last().firstChild.attributes.getNamedItem("href").textContent

                //construct the next page url
                val nextPageUrl = url + nextPageArgs

                //parse data from the current page
                val gameCards = page.querySelectorAll("div.card-panel.sale-list-item")
                gameCards.forEach{
                    val titleHref = it.querySelector<DomNode>("div.sale-list-action > a[href]")
                    val url = titleHref.attributes.getNamedItem("href").textContent

                    //find out if game here

                    val name = it.querySelector<DomNode>("div.app-info > p.app-name").asText()
                    val pricing = it.querySelector<DomNode>("div.pricing")
                    val priceNew = pricing.querySelector<DomNode>("div.price-new").asText()
                    val priceOld = pricing.querySelector<DomNode>("div.price-old").asText()

                    val card = Card(url, name, priceOld, priceNew)
                    println(url)
                    list.add(card)
                }

                //update currUrl, prevPage and currPage
                currUrl = nextPageUrl
            }
            println("Found ${list.size} records")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
