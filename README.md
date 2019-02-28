# pleiepengesoknad-prosessering

Tjeneste som prosesserer søknad om pleiepenger.
Skal konsumere kafka topic "privat-pleiepengesoknad" og prosessere søknad basert på innhold på denne køen.
Kan også sende samme request som kommer på kafka-topic som et REST API-kall til tjenesten.

Første versjon av tjenesten vil motta og prossesere søknader synkront, men med et mål om å gjøre det asynkront ved hjelp av Kafka.

## Versjon 1
### Meldingsformat
- Gir 202 response
- soker.mellomnavn er ikke påkrevd.
- ingen av attributtene for "barn" er påkrevd.
- arbeidsgivere kan være en tom liste
- arbeidsgivere[x].navn er ikke påkrevd.
- vedlegg_urls[x] kan inneholde en URL til vedlegg i tjenesten "pleiepenger-dokumenter" og peke på dokument som tilhører søkeren.
- vedlegg[x] kan inneholde en JSON med vedlegg på format som vist i eksempel hvor "content" er base64 encoded vedlegg
- Det må være satt enten en entry i dokument_urls eller vedlegg

```json
{
    "mottatt": "2019-02-15T20:43:32Z",
	"fra_og_med": "2018-10-10",
	"til_og_med": "2019-10-10",
	"soker": {
		"fodselsnummer": "290990123456",
		"fornavn": "MOR",
		"mellomnavn": "HEISANN",
		"etternavn": "MORSEN"
	},
	"barn": {
		"fodselsnummer": "25099012345",
		"alternativ_id": null,
		"navn": "Santa Heisann Winter"
	},
	"relasjon_til_barnet" : "MOR",
	"arbeidsgivere": {
		"organisasjoner": [{
			"navn": "Bjeffefirmaet",
			"organisasjonsnummer": "897895478"
		}]
	},
	"vedlegg_urls": [
		"http://localhost:8133/v1/dokument/eyJraWQiOiIxIiwidHlwIjoiSldUIiwiYWxnIjoibm9uZSJ9.eyJqdGkiOiJlZWNlY2NkNS1jNjkyLTQyMDEtYTJhNi04OTFiMWFjZDYwMGMifQ"
	],
	"vedlegg": [{
    	"content": "iVBORw0KGg....ayne82ZEAAAAASUVORK5CYII=",
    	"content_type": "image/png",
    	"title": "Legeerklæring"
    }],
	"medlemskap" : {
        "har_bodd_i_utlandet_siste_12_mnd" : false,
        "skal_bo_i_utlandet_neste_12_mnd" : false
	}
}
```

### Metadata
#### Correlation ID vs Request ID
Correlation ID blir propagert videre, og har ikke nødvendigvis sitt opphav hos konsumenten.
Request ID blir ikke propagert videre, og skal ha sitt opphav hos konsumenten.

#### REST API
- Correlation ID må sendes som header 'X-Correlation-ID'
- Request ID kan sendes som heder 'X-Request-ID'
- Versjon på meldingen avledes fra pathen '/v1/soknad' -> 1


#### Kafka
- Correlation ID må sendes som header til meldingen med navn 'X-Correlation-Id'
- Request ID kan sendes som header til meldingen med navn 'X-Correlation-Id'
- Versjon på meldingen må sendes som header til meldingen med navn 'X-Nav-Message-Version'

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
