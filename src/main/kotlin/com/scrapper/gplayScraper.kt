package com.scrapper

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.HtmlPage

import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.Klaxon
import com.gargoylesoftware.htmlunit.ScriptResult
import com.gargoylesoftware.htmlunit.javascript.host.dom.Text
import net.sourceforge.htmlunit.corejs.javascript.Script
import netscape.javascript.JSObject
import java.applet.Applet


class gplayScraper(){
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

    private fun filterTextForComparison(text: String): String? {

        var filteredText: String? = text

        if (filteredText != null) {
            filteredText = filteredText.replace("\\p{Cc}".toRegex(), " ").replace("\\s{2,}".toRegex(), " ")
        }

        return filteredText
    }

    fun isGame(url : String) : Boolean{
        val matchScriptData = """
            function matchScriptData (response) {
              const scriptRegex = />AF_initDataCallback[\s\S]*?<\/script/g;
              const keyRegex = /(ds:.*?)'/;
              const valueRegex = /return ([\s\S]*?)}}\);<\//;

              const ret = response.match(scriptRegex)
                .reduce((accum, data) => {
                  const keyMatch = data.match(keyRegex);
                  const valueMatch = data.match(valueRegex);


                  if (keyMatch && valueMatch) {
                    const key = keyMatch[1];
                    const value = JSON.parse(valueMatch[1]);
                    accum[key] = value
                  }
                  return accum;
                }, {});
                return ret
            }
            """
        val MAPPINGS = """
             const MAPPINGS = {
              // FIXME add appId

              title: ['ds:4', 0, 0, 0],
              description: {
                path: ['ds:4', 0, 10, 0, 1],
                fun: descriptionText
              },
              descriptionHTML: ['ds:4', 0, 10, 0, 1],
              summary: ['ds:4', 0, 10, 1, 1],
              installs: ['ds:4', 0, 12, 9, 0],
              minInstalls: {
                path: ['ds:4', 0, 12, 9, 0],
                fun: cleanInt
              },
              score: ['ds:6', 0, 0, 1],
              scoreText: ['ds:6', 0, 0, 0],
              ratings: ['ds:6', 0, 2, 1],
              reviews: ['ds:6', 0, 3, 1],
              histogram: {
                path: ['ds:6', 0, 1],
                fun: buildHistogram
              },

              price: {
                path: ['ds:10', 0, 2, 0, 0, 0, 1, 0, 0],
                fun: (val) => val / 1000000 || 0
              },
              free: {
                path: ['ds:10', 0, 2, 0, 0, 0, 1, 0, 0],
                // considered free only if prize is exactly zero
                fun: (val) => val === 0
              },
              currency: ['ds:10', 0, 2, 0, 0, 0, 1, 0, 1],
              priceText: {
                path: ['ds:10', 0, 2, 0, 0, 0, 1, 0, 2],
                fun: priceText
              },
              offersIAP: {
                path: ['ds:4', 0, 12, 12, 0],
                fun: Boolean
              },

              size: ['ds:7', 0],
              androidVersion: {
                path: ['ds:7', 2],
                fun: normalizeAndroidVersion
              },
              androidVersionText: ['ds:7', 2],

              developer: ['ds:4', 0, 12, 5, 1],
              developerId: {
                path: ['ds:4', 0, 12, 5, 5, 4, 2],
                fun: (devUrl) => devUrl.split('id=')[1]
              },
              developerEmail: ['ds:4', 0, 12, 5, 2, 0],
              developerWebsite: ['ds:4', 0, 12, 5, 3, 5, 2],
              developerAddress: ['ds:4', 0, 12, 5, 4, 0],
              privacyPolicy: ['ds:4', 0, 12, 7, 2],
              genre: ['ds:4', 0, 12, 13, 0, 0],
              genreId: ['ds:4', 0, 12, 13, 0, 2],
              familyGenre: ['ds:4', 0, 12, 13, 1, 0],
              familyGenreId: ['ds:4', 0, 12, 13, 1, 2],

              icon: ['ds:4', 0, 12, 1, 3, 2],
              headerImage: ['ds:4', 0, 12, 2, 3, 2],
              screenshots: {
                path: ['ds:4', 0, 12, 0],
                fun: R.map(R.path([3, 2]))
              },
              video: ['ds:4', 0, 12, 3, 0, 3, 2],
              videoImage: ['ds:4', 0, 12, 3, 1, 3, 2],

              contentRating: ['ds:4', 0, 12, 4, 0],
              contentRatingDescription: ['ds:4', 0, 12, 4, 2, 1],
              adSupported: {
                path: ['ds:4', 0, 12, 14, 0],
                fun: Boolean
              },

              released: ['ds:4', 0, 12, 36],
              updated: {
                path: ['ds:4', 0, 12, 8, 0],
                fun: (ts) => ts * 1000
              },

              version: ['ds:7', 1],
              recentChanges: ['ds:4', 0, 12, 6, 1],
              comments: {
                path: ['ds:14', 0],
                fun: extractComments
              }

            };
             """
        //has R dependency
        val extractFields = """
            function extractFields (parsedData) {
              return R.map((spec) => {
                if (R.is(Array, spec)) {
                  return R.path(spec, parsedData);
                }
                // assume spec object
                const input = R.path(spec.path, parsedData);
                return spec.fun(input);
              }, MAPPINGS);
            }
            """
        //has cheerio dependency
        val descriptionText = """
            function descriptionText (description) {
              // preserve the line breaks when converting to text
              const html = cheerio.load('<div>' + description.replace(/<br>/g, '\r\n') + '</div>');
              //console.log(cheerio.text(html('div')))
              return cheerio.text(html('div'));
            }
            """
        val priceText = """
            function priceText (priceText) {
              // Return Free if the price text is empty
              if (!priceText) {
                return 'Free';
              }
              return priceText;
            }
            """
        val cleanInt = """
            function cleanInt (number) {
              number = number || '0';
              number = number.replace(/[^\d]/g, ''); // removes thousands separator
              return parseInt(number);
            }
            """
        val normalizeAndroidVersion = """
            function normalizeAndroidVersion (androidVersionText) {
              const number = androidVersionText.split(' ')[0];
              if (parseFloat(number)) {
                return number;
              }
              return 'VARY';
            }
            """
        val buildHistogram = """
            function buildHistogram (container) {
              if (!container) {
                return { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
              }
              return {
                1: container[1][1],
                2: container[2][1],
                3: container[3][1],
                4: container[4][1],
                5: container[5][1]
              };
            }
            """
        val extractComments = """
            function extractComments (comments) {
              if (!comments) {
                return [];
              }
              return R.compose(
                R.take(5),
                R.reject(R.isNil),
                R.pluck(3))(comments);
            }
            """

        val page = client.getPage<HtmlPage>(url)
        val rawPageString = page.webResponse.contentAsString
        val pageString = rawPageString.replace("'", "\\'")
        val appIdIndex = url.indexOf("?id=")
        if(appIdIndex == -1) {
            throw Exception("id parameter not found in given url $url")
        }
        val appId = url.substring(appIdIndex+4)
        println("The appId is: $appId")
        val app = """
            $matchScriptData
            function app (htmlString) {
                const matchedScriptData = matchScriptData(htmlString)
                return matchedScriptData
            }
            """

        val cleanedHtmlString = filterTextForComparison(pageString)
        println(cleanedHtmlString)
        val result = page.executeJavaScript("""
            $app
            app('$cleanedHtmlString')
            """)
        println(result.javaScriptResult)
        return true
        //      how final version should look like
//        val app = """
//            $matchScriptData
//            $extractFields
//            $MAPPINGS
//            $descriptionText
//            $priceText
//            $cleanInt
//            $normalizeAndroidVersion
//            $buildHistogram
//            $extractComments
//            function app (htmlString) {
//
//                const matchedScriptData = matchScriptData(htmlString)
//                return matchedScriptData
//                //const extractedFields = extractFields(matchedScriptData)
//                //const result = Object.assign(extractedFields, {appId: opts.appId, url: '$url'})
//                //return result
//            }
//            """
    }


}

fun main(args : Array<String>){
    val scrapper = gplayScraper()
    println("Program running")
    scrapper.isGame("https://play.google.com/store/apps/details?id=com.dxco.pandavszombies")
}