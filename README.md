# pleiepenger-sak

Inneholder integrasjon mot sak for å opprette sak i forbindelse med søknad om Pleiepenger.
Skal konsumere fra kafka-topic og opprette sak. Videre skal den legge en ny entry på en annen Topic for å opprette en jorunalføring (pleiepenger-joark)
Kan også sende samme request som kommer på kafka-topic som et REST API-kall til tjenesten.

## Versjon 1
### Meldingsformat
- aktoer_id : AtkørID for personen saken skal opprettes på

```json
{
    "aktoer_id": "123123"
}
```

### Metadata
#### Correlation ID vs Request ID
Correlation ID blir propagert videre, og har ikke nødvendigvis sitt opphav hos konsumenten
Request ID blir ikke propagert videre, og skal ha sitt opphav hos konsumenten

#### REST API
- Correlation ID må sendes som header 'X-Correlation-ID'
- Request ID kan sendes som heder 'X-Request-ID'
- Versjon på meldingen avledes fra pathen '/v1/sak' -> 1


#### Kafka
- Correlation ID må sendes som header til meldingen med navn 'X-Correlation-Id'
- Request ID kan sendes som header til meldingen med navn 'X-Correlation-Id'
- Versjon på meldingen må sendes som header til meldingen med navn 'X-Nav-Message-Version'

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
