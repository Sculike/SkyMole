# Third-party content

SkyMole itself is [MIT licensed](LICENSE). It also includes data from other projects:

## SkyHanni-REPO (MIT)

The marker locations in `src/main/resources/assets/skymole/markers.json` and the Dwarven Mines
walking network in `src/main/resources/assets/skymole/routes/` are based on
[SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO), used under the MIT License.

The full license text is in
[`src/main/resources/assets/skymole/markers-LICENSE.txt`](src/main/resources/assets/skymole/markers-LICENSE.txt).

SkyMole's own measured Dwarven Mines commission spots are not from that data.

## Prices

Prices are fetched at runtime from public APIs:

- Hypixel's official API (`api.hypixel.net`) for bazaar prices and the item list
- moulberry's lowest-BIN list (`moulberry.codes`) for auction items
- Coflnet's SkyBlock API (`sky.coflnet.com`) for single auction items, used when that list is
  unavailable

SkyMole only reads these; it sends no data about you anywhere.
