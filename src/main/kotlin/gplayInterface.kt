import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.lang.ProcessBuilder


class GplayInterface{
    class StreamGobbler(val inputStream : InputStream, val type : String) : Runnable{
        override fun run(){
            val br = BufferedReader(InputStreamReader(inputStream))
            var line = br.readLine()
            while(line != null){
                println(line)
                line = br.readLine()
            }
//            BufferedReader(InputStreamReader(inputStream)).lines().forEach({
//                it -> println(it)
//            })
        }
    }

    fun getIdFromUrl(url : String) : String{
        val appIdIndex = url.indexOf("?id=")
        var appIdEndIndex = url.indexOf("&")
        if(appIdIndex == -1) {
            throw Exception("id parameter not found in given url $url")
        }
        appIdEndIndex = if(appIdEndIndex == -1) url.length else appIdEndIndex

        return url.substring(appIdIndex+4,appIdEndIndex)
    }

    fun isGame(url : String){
        val id = getIdFromUrl(url)
        try{
            val pb = ProcessBuilder("node", "test.js", "--id", id)
            pb.directory(File("/home/msoerjanto/IdeaProjects/sample.project/isGame"))
            val process = pb.start()
            val outputGobbler = StreamGobbler(process.inputStream, "output")
            val errorGobbler = StreamGobbler(process.errorStream, "error")

            Executors.newSingleThreadExecutor().submit(outputGobbler)
            Executors.newSingleThreadExecutor().submit(errorGobbler)

            val exitCode = process.waitFor()
            assert(exitCode == 0)
        }catch(e : Exception){
            e.printStackTrace()
        }
    }
}

fun main(args : Array<String>){
    GplayInterface().isGame("https://play.google.com/store/apps/details?id=com.facebook.orca&hl=en_US")
}