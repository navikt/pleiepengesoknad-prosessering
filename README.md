# pleiepengesoknad-prosessering
![CI / CD](https://github.com/navikt/pleiepengesoknad-prosessering/workflows/CI%20/%20CD/badge.svg)
![CI / CD](https://github.com/navikt/pleiepengesoknad-prosessering/workflows/Alerts/badge.svg)

Tjeneste som prosesserer søknader om pleiepenger.
Leser søknader fra Kafka topic `privat-pleiepengesoknad-mottatt-v2` som legges der av [pleiepengesoknad-api](https://github.com/navikt/pleiepengesoknad-api)

## Prosessering
- Genererer Søknad-PDF
- Oppretter Journalpost
- Oppretter Gosys Oppgave
- Sletter mellomlagrede dokumenter

## Feil i prosessering
Ved feil i en av streamene som håndterer prosesseringen vil streamen stoppe, og tjenesten gi 503 response på liveness etter 15 minutter.
Når tjenenesten restarter vil den forsøke å prosessere søknaden på ny og fortsette slik frem til den lykkes.

## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerterator.yml](nais/alerterator.yml).

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #team-düsseldorf.
