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

Prices are fetched at runtime from Hypixel's public API (`api.hypixel.net`) and from
moulberry's public lowest-BIN list (`moulberry.codes`). No data is sent anywhere; SkyMole only
reads these.
