# pleiepenger-sak

Inneholder integrasjon mot sak for å opprette sak i forbindelse med søknad om Pleiepenger.
Skal konsumere fra kafka-topic og opprette sak. Videre skal den legge en ny entry på en annen Topic for å opprette en jorunalføring (pleiepenger-joark)
Kan også sende samme request som kommer på kafka-topic som et REST API-kall til tjenesten.

## Versjon 1
### Meldingsformat
TODO

### Metadata
#### REST API
- Correlation ID må sendes som header 'Nav-Call-Id'
- Versjon på meldingen avledes fra pathen '/v1/sak' -> 1

#### Kafka
- Correlation-ID må sendes som header til meldingen med navn 'Nav-Call-Id'
- Versjon på meldingen må sendes som header til meldingen med navn 'Nav-Message-Version'

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
