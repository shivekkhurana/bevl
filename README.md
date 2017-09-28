## BEVL - Leyton Bikes Viewer App

BEVL is a simple clojure app to show availbility of bikes around Leyton. 
It depends on the [Traffic for London Unified API](https://api.tfl.gov.uk), particularly [the bikepoints api](https://api.tfl.gov.uk/swagger/ui/index.html?url=/swagger/docs/v1#!/BikePoint/BikePoint_GetAll).
It renders a web page protected by Basic Authentication and shows a table of 5 nearest bikepoints around Leyton.

**DEMO**

A screencast of the app is available at http://jmp.sh/DG64sEI


**How it works ?**

- BEVL has the coordinates of Leyton stored in config. 
- It fetches all bikepoints from the api mentioned above and sorts them by `haversine` distance between the bikepoint and Leyton.
- Next it picks the top 5 bikepoints and renders them to html (with some styling).


**Dependencies**

- [tachyons](http://tachyons.io) functional css for ui rendering 
- [clj-http](https://github.com/dakrone/clj-http) for sending request to TFL api
- [hiccup](https://github.com/weavejester/hiccup) for generating html
- [yada](https://github.com/juxt/yada) for handling resources
- [minimal-yada-bidi](https://github.com/kornysietsma/minimal-yada-bidi) boilerplate
- [haversine](https://github.com/ThomasMeier/haversine) for calculating distances between lat and lngs
- [aero](https://github.com/juxt/yada) for config management
- [cheshire](https://github.com/dakrone/cheshire) for json parsing


**Setup**

Clone this repo
```
git clone git@github.com:shivekkhurana/bevl.git
```

Run the server
```
lein run
```

Browse to http://localhost:3000
