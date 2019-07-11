# pleiepengesoknad-prosessering

Tjeneste som prosesserer søknader om pleiepenger.
Leser søknader fra Kafka topic ```privat-pleiepengesoknad-mottatt`` som legges der av [pleiepengesoknad-mottak](https://github.com/navikt/pleiepengesoknad-mottak)

## Prosessering
- Genererer Søknad-PDF
- Oppretter Journalpost
- Oppretter Gosys Oppgave
- Sletter mellomlagrede dokumenter

## Feil i prosessering
Ved feil i en av streamene som håndterer prosesseringen vil streamen stoppe, og tjenesten gi 503 response på liveness etter 15 minutter.
Når tjenenesten restarter vil den forsøke å prosessere søknaden på ny og fortsette slik frem til den lykkes.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-düsseldorf
