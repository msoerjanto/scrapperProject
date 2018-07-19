package com.scrapper
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.HtmlPage


data class Card(val url : String, val name : String, val fp : String, val cp : String)

class Scrapper{
    fun scrap(){
        println("Project Started")
        val client = WebClient(BrowserVersion.CHROME)
        client.options.isCssEnabled = false
        client.options.isJavaScriptEnabled = false
        client.options.isUseInsecureSSL = true

        val url = "https://www.app-sales.net/nowfree/"

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
                    val name = it.querySelector<DomNode>("div.app-info > p.app-name").asText()
                    val pricing = it.querySelector<DomNode>("div.pricing")
                    val priceNew = pricing.querySelector<DomNode>("div.price-new").asText()
                    val priceOld = pricing.querySelector<DomNode>("div.price-old").asText()

                    val card = Card(url, name, priceOld, priceNew)
                    println(card)
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

//fun main(args : Array<String>){
//    val scrapper = Scrapper()
//    scrapper.scrap()
//}