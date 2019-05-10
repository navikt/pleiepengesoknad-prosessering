# Topics
Beskriver flyten fra når denne tjeneste (pleiepengesoknad-prosessering) får inn en søknad til den er registrert i et saksbehandlingsystem.

## privat-pleiepenger-soknadMottatt
* Producer i tjenesten 'pleiepengesoknad-prosessering'
Først mottar vi søknaden med vedlegg (byte arrays) i et REST-endepunkt, mellomlagrer disse sammen med generert PDF av søknaden og legger en entry på topic som inneholder URL’er til å hente dokumenter sammen med informasjonen som ligger i selve søknaden. (JSON)
Når denne er blitt lagt på topicen returnerer vi 202 - Søknad mottatt og lagt til prosessering

* Consumer I tjenesten ‘pleiepenger-joark’
Journalfører alle dokumenter i meldingen.

## privat-pleiepenger-joarkJournalPostOpprettet
* Producer i tjenesten ‘pleiepenger-joark’
Når alle dokumenter er journalført legges til en entry på topic som inneholder ‘journal_post_id’
* Consumer I tjenesten ‘pleiepenger-saksbehandler’
Merger 'privat-pleiepenger-joarkJournalPostOpprettet’ og 'privat-pleiepenger-soknadMottatt’
Når det fines en matchene entry på disse topicene.
I første omgang vil den kun sende det Mergede resultatet til 'privat-pleiepenger-maaBehandlesIGosysOgInfotrygd'


## privat-pleiepenger-maaBehandlesIGosysOgInfotrygd'
* Producer I ‘pleiepenger-saksbehandler’ (Se over)
* Consumer I ‘pleiepenger-oppgave’
Oppretter oppgave i Gosys

