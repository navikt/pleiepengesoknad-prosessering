# pleiepenger-joark

Inneholder integrasjon mot joark for å opprette jornalpost i forbindelse med søknad om Pleiepenger.
Skal konsumere fra kafka-topic og journalføre i Joark.
Kan også sende samme request som kommer på kafka-topic som et REST API-kall til tjenesten.

## Versjon 1
### Meldingsformat
- aktoer_id : AtkørID for personen dokumentene skal journalføres på
- mottatt : tidspunkt for når dokumentene er mottatt på ISO8601 format
- saks_id : Id opprettet ved opprettelse av sak (pleiepenger-sak)
- dokumenter : En liste med dokumenter som skal journalføres. Første dokument i listen vil bli "Hoveddokument" i Joark
- dokument.tittel : Tittel for dokumentet
- dokument.content_type : Content-Type for dokumentet. Per nå støtte kun 'application/pdf'
- dokument.innhold : Base64-encoded / byte array av dokumentet

```json
{
	"aktoer_id":"123561458",
	"mottatt": "2018-12-18T20:43:32Z",
	"saks_id": "1234654",
	"dokumenter": [{
		"tittel": "Hoveddokument",
		"content_type": "application/pdf",
		"innhold": "ey123445641235544224"
	}]
}
```

### Metadata
#### REST API
- Correlation ID må sendes som header 'Nav-Call-Id'
- Versjon på meldingen avledes fra pathen '/v1/journalforing' -> 1

#### Kafka
- Correlation-ID må sendes som header til meldingen med navn 'Nav-Call-Id'
- Versjon på meldingen må sendes som header til meldingen med navn 'Nav-Message-Version'

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
