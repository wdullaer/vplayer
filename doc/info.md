https://secvod.stream.vrt.be
https://mediazone.vrt.be

// Fetch url with .mssecurevideo.json as extension
// Authentication seems to work by passin in a cookie to the following request
https://www.vrt.be/vrtnu/a-z/100-jaar-grote-oorlog--herdenking-slag-bij-polygoonbos/2017/100-jaar-grote-oorlog--herdenking-slag-bij-polygoonbos-s2017.mssecurevideo.json
{"/content/dam/vrt/2017/10/04/100-jaar-grote-oorlog-herdenking-slag-bij-polygoonbos-depot_WP00118416":{"videoid":"pbs-pub-09c4d0ee-936f-4ec1-b79d-b0ad2088b2c2$vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec","clientid":"vrtvideo"}}
// TODO: figure out how the authentication cookie is obtained and how to persist it in the app
// parse clientid
clientid // : 'vrtvideo'
// parse videoid
videoid // is the assetid in the eventual stream url

api/v1/%%CLIENT%%/assets/%%ASSET%%

// Example:
https://mediazone.vrt.be/api/v1/vrtvideo/assets/pbs-pub-09c4d0ee-936f-4ec1-b79d-b0ad2088b2c2$vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec
{"title":"100 jaar grote oorlog: herdenking Slag bij Polygoonbos","description":"De Australische overheid organiseert in samenwerking met de Vlaamse overheid, de gemeente Zonnebeke en het Memorial Museum Passchendaele 1917 een ochtendplechtigheid om honderd jaar Slag bij Polygoonbos te herdenken.   De verovering van het Doelbos en van de 'Butte' in Zonnebeke was een van de belangrijkste objectieven van de strijd van de Australiërs op 26 september 1917. Van de 2110 soldaten die op de begraafplaats zijn begraven, waaronder 560 Australiërs, zijn meer dan 1600 onbekend.   Voor de Australiërs is het na Gallipoli de grootste herdenkingsplechtigheid in Europa; zo'n 1000 gasten worden speciaal voor de gelegenheid overgevlogen.","duration":4005080,"aspectRatio":"16:9","metaInfo":{"allowedRegion":"WORLD","ageCategory":"","whatsonId":"460389142527"},"targetUrls":[{"type":"HLS","url":"https://ondemand-w.lwc.vrtcdn.be/content/vod/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1_nodrm_acfa4f1e-0829-47da-9f14-56e42852f14b.ism/.m3u8"},{"type":"MPEG_DASH","url":"https://ondemand-w.lwc.vrtcdn.be/content/vod/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1_nodrm_acfa4f1e-0829-47da-9f14-56e42852f14b.ism/.mpd"},{"type":"HSS","url":"https://ondemand-w.lwc.vrtcdn.be/content/vod/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1_nodrm_acfa4f1e-0829-47da-9f14-56e42852f14b.ism/Manifest"},{"type":"HDS","url":"https://ondemand-w.lwc.vrtcdn.be/content/vod/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1/vid-fb0d7ade-0d4e-49ae-afa8-dbae790b62ec-CDN_1_nodrm_acfa4f1e-0829-47da-9f14-56e42852f14b.ism/.f4m"}],"subtitleUrls":[],"posterImageUrl":"https://images.vrt.be/orig/2017/10/04/15a72905-a900-11e7-bbe7-02b7b76bf47f.jpg","tags":[]}

'https://search.vrt.be/suggest?i=video&q=hand'
[
  {
    "title": "The Handmaid's tale",
    "type": "program",
    "episode_count": 10,
    "score": 27.339529,
    "targetUrl": "//www.vrt.be/vrtnu/a-z/the-handmaid-s-tale.relevant/",
    "programName": "the-handmaid-s-tale",
    "thumbnail": "//images.vrt.be/orig/2018/05/09/fab40d9a-536b-11e8-abcc-02b7b76bf47f.jpg",
    "brands": [
      "canvas"
    ],
    "description": "<p>Het dystopische verhaal van de dienstmaagden in Gilead</p>\n"
  }
]

// Seems like type "MPEG_DASH" is the one that will work best (or HDS)
// Will need integration of exoPlayer in the app
https://google.github.io/ExoPlayer/guide.html
https://github.com/googlesamples/androidtv-Leanback // Uses exoPlayer, should convert this to kotlin
