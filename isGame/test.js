const gplay = require('google-play-scraper')


const gameUrl = 'com.pocketgems.android.dragon'
const nonGameUrl = 'com.facebook.orca'
const pandaVzombie = 'com.dxco.pandavszombies'

//setup option handling
const argv = require('yargs')
              .usage('Usage: $0 --id [string]')
              .demandOption(['id'])
              .argv


const isGame = async (url) => {
  const result = await gplay.app({appId: url})
  if(!result)
    throw new Error(`unable to fetch app with id ${url}`)
  return (result.genreId.substring(0,4) === 'GAME') ? true : false
}

// const isGame = (url) => {
//   return new Promise((resolve, reject) => {
//     gplay.app({appId:url}).then((res) => {
//       console.log('successfully retrieved data')
//       if(!res)
//         reject(`cant get object from id ${url}`)
//       const result = (res.genreId.substring(0,4) === 'GAME') ? true : false
//       resolve(result)
//     })
//   })
// }

isGame(argv.id).then((res) => {
  if(res)
    process.stdout.write('true')
  else
    process.stdout.write('false')
  
  process.exit()
}).catch((err) => console.log(err))
