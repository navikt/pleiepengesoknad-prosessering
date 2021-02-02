package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v2.InternInfo
import no.nav.helse.prosessering.v2.InternSøker
import no.nav.helse.prosessering.v2.MeldingV2
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.LovbestemtFerie
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.aktivitet.*
import no.nav.k9.søknad.felles.aktivitet.VirksomhetType.*
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdPeriodeInfo
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap.BeredskapPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.NattevåkPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.SøknadInfo
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9.søknad.ytelse.psb.v1.UttakPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import no.nav.k9.søknad.felles.personopplysninger.Barn as K9Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker as K9Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold as K9Utenlandsopphold
import no.nav.k9.søknad.felles.type.Periode as K9Periode
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap as K9Beredskap
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk as K9Nattevåk
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning as K9Tilsynsordning

internal object SøknadUtils {
    internal val objectMapper = jacksonObjectMapper().dusseldorfConfigured()
    private val start = LocalDate.parse("2020-01-01")
    private const val GYLDIG_ORGNR = "917755736"

    internal val defaultSøknad = MeldingV1(
        språk = "nb",
        søknadId = UUID.randomUUID().toString(),
        mottatt = ZonedDateTime.now(),
        fraOgMed = LocalDate.now(),
        tilOgMed = LocalDate.now().plusWeeks(1),
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = "02119970078",
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        barn = Barn(
            navn = "Ole Dole",
            fødselsnummer = "19066672169",
            fødselsdato = LocalDate.now().minusYears(10),
            aktørId = "123456"
        ),
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = listOf(
                Organisasjon(
                    "917755736",
                    "Gyldig",
                    jobberNormaltTimer = 4.0,
                    skalJobbeProsent = 50.0,
                    skalJobbe = "redusert"
                ),
                Organisasjon(
                    "917755734",
                    "Gyldig",
                    jobberNormaltTimer = 40.0,
                    skalJobbeProsent = 40.0,
                    skalJobbe = "ja"
                ),
                Organisasjon(
                    "917755734",
                    "Gyldig",
                    jobberNormaltTimer = 8.0,
                    skalJobbeProsent = 0.0,
                    skalJobbe = "nei"
                ),
                Organisasjon(
                    "917755734",
                    "Gyldig",
                    jobberNormaltTimer = 40.0,
                    skalJobbeProsent = 40.0,
                    skalJobbe = "vetIkke"
                )
            )
        ),
        vedleggUrls = listOf(URI("http://localhost:8080/vedlegg/1")),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            utenlandsoppholdSiste12Mnd = listOf(
                Bosted(
                    LocalDate.of(2020, 1, 2),
                    LocalDate.of(2020, 1, 3),
                    "US", "USA"
                )
            ),
            skalBoIUtlandetNeste12Mnd = false
        ),
        harMedsøker = true,
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true,
        tilsynsordning = Tilsynsordning(
            svar = "ja",
            ja = TilsynsordningJa(
                mandag = Duration.ofHours(5),
                tirsdag = Duration.ofHours(4),
                onsdag = Duration.ofHours(3).plusMinutes(45),
                torsdag = Duration.ofHours(2),
                fredag = Duration.ofHours(1).plusMinutes(30),
                tilleggsinformasjon = "Litt tilleggsinformasjon."
            ),
            vetIkke = null
        ),
        beredskap = Beredskap(
            beredskap = true,
            tilleggsinformasjon = "I Beredskap"
        ),
        nattevåk = Nattevåk(
            harNattevåk = true,
            tilleggsinformasjon = "Har Nattevåk"
        ),
        utenlandsoppholdIPerioden = UtenlandsoppholdIPerioden(
            skalOppholdeSegIUtlandetIPerioden = true,
            opphold = listOf(
                Utenlandsopphold(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-10"),
                    landnavn = "Bahamas",
                    landkode = "BAH",
                    erUtenforEøs = true,
                    erBarnetInnlagt = true,
                    perioderBarnetErInnlagt = listOf(
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-01"),
                            tilOgMed = LocalDate.parse("2020-01-01")
                        ),
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-03"),
                            tilOgMed = LocalDate.parse("2020-01-04")
                        )
                    ),
                    årsak = Årsak.ANNET
                ),
                Utenlandsopphold(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-10"),
                    landnavn = "Svergie",
                    landkode = "BHS",
                    erUtenforEøs = false,
                    erBarnetInnlagt = true,
                    perioderBarnetErInnlagt = listOf(
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-01"),
                            tilOgMed = LocalDate.parse("2020-01-01")
                        ),
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-03"),
                            tilOgMed = LocalDate.parse("2020-01-04")
                        ),
                        Periode(
                            fraOgMed = LocalDate.parse("2020-01-05"),
                            tilOgMed = LocalDate.parse("2020-01-05")
                        )
                    ),
                    årsak = Årsak.ANNET
                )
            )
        ),
        ferieuttakIPerioden = FerieuttakIPerioden(
            skalTaUtFerieIPerioden = true,
            ferieuttak = listOf(
                Ferieuttak(LocalDate.parse("2020-01-07"), LocalDate.parse("2020-01-08")),
                Ferieuttak(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-01-10"))
            )
        ),
        frilans = Frilans(
            startdato = LocalDate.now().minusYears(3),
            jobberFortsattSomFrilans = true
        ),
        selvstendigVirksomheter = listOf(
            Virksomhet(
                næringstyper = listOf(Næringstyper.ANNEN),
                fraOgMed = LocalDate.parse("2021-01-01"),
                tilOgMed = LocalDate.parse("2021-01-10"),
                navnPåVirksomheten = "Kjells Møbelsnekkeri",
                registrertINorge = true,
                yrkesaktivSisteTreFerdigliknedeÅrene = YrkesaktivSisteTreFerdigliknedeÅrene(LocalDate.parse("2021-01-01")),
                organisasjonsnummer = "111111"
            ), Virksomhet(
                næringstyper = listOf(Næringstyper.JORDBRUK_SKOGBRUK),
                organisasjonsnummer = "9999",
                fraOgMed = LocalDate.parse("2020-01-01"),
                navnPåVirksomheten = "Kjells Skogbruk",
                registrertINorge = false,
                registrertIUtlandet = Land(
                    "DEU",
                    "Tyskland"
                ),
                næringsinntekt = 900_000,
                regnskapsfører = Regnskapsfører(
                    "Bård",
                    "98989898"
                )
            )
        )
    )

    val defaultK9FormatPSB = Søknad(
        SøknadId.of(UUID.randomUUID().toString()),
        Versjon.of("2.0.0"),
        ZonedDateTime.now(),
        K9Søker.builder()
            .norskIdentitetsnummer(NorskIdentitetsnummer.of("12345678910"))
            .build(),
        PleiepengerSyktBarn(
            K9Periode(LocalDate.now().minusMonths(1), LocalDate.now()),
            SøknadInfo(
                "Far",
                true,
                "beskriver omsorgsrollen...",
                true,
                true,
                true,
                true,
                true,
                true
            ),
            K9Barn(
                NorskIdentitetsnummer.of("10987654321"),
                null
            ),
            ArbeidAktivitet.builder()
                .frilanser(Frilanser(LocalDate.now().minusMonths(9), true))
                .selvstendigNæringsdrivende(
                    listOf(
                        SelvstendigNæringsdrivende(
                            mapOf(
                                K9Periode(
                                    LocalDate.now().minusMonths(9),
                                    LocalDate.now()
                                ) to SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
                                    .erNyoppstartet(true)
                                    .registrertIUtlandet(false)
                                    .bruttoInntekt(BigDecimal(5_000_000))
                                    .erVarigEndring(true)
                                    .endringDato(LocalDate.now().minusMonths(4))
                                    .endringBegrunnelse("Grunnet Covid-19")
                                    .landkode(Landkode.NORGE)
                                    .regnskapsførerNavn("Regnskapsfører Svensen")
                                    .regnskapsførerTelefon("+4799887766")
                                    .virksomhetstyper(listOf(DAGMAMMA, ANNEN))
                                    .build()
                            ),
                            Organisasjonsnummer.of("12345678910112233444455667"),
                            "Mamsen Bamsen AS"
                        ),
                        SelvstendigNæringsdrivende(
                            mapOf(
                                K9Periode(
                                    LocalDate.now().minusYears(5),
                                    LocalDate.now()
                                ) to SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
                                    .erNyoppstartet(false)
                                    .registrertIUtlandet(true)
                                    .bruttoInntekt(BigDecimal(500_000))
                                    .erVarigEndring(false)
                                    .endringDato(null)
                                    .endringBegrunnelse(null)
                                    .landkode(Landkode.SPANIA)
                                    .regnskapsførerNavn(null)
                                    .regnskapsførerTelefon(null)
                                    .virksomhetstyper(listOf(FISKE))
                                    .build()
                            ),
                            Organisasjonsnummer.of("54549049090490498048940940"),
                            "Something Fishy AS"
                        ),
                    )
                )
                .build(),
            K9Beredskap(
                mapOf(
                    K9Periode(
                        LocalDate.now().minusDays(5),
                        LocalDate.now()
                    ) to BeredskapPeriodeInfo("Jeg skal være i beredskap. Basta!"),
                    K9Periode(
                        LocalDate.now(),
                        LocalDate.now().plusDays(5)
                    ) to BeredskapPeriodeInfo("Jeg skal være i beredskap i denne perioden også. Basta!")
                )
            ),
            K9Nattevåk(
                mapOf(
                    K9Periode(
                        LocalDate.now().minusDays(5),
                        LocalDate.now()
                    ) to NattevåkPeriodeInfo("Jeg skal ha nattevåk. Basta!"),
                    K9Periode(
                        LocalDate.now(),
                        LocalDate.now().plusDays(5)
                    ) to NattevåkPeriodeInfo("Jeg skal ha nattevåk i perioden også. Basta!")
                )
            ),
            K9Tilsynsordning(
                mapOf(
                    K9Periode(
                        LocalDate.now().minusDays(5),
                        LocalDate.now()
                    ) to TilsynPeriodeInfo(Duration.ofHours(8)),
                    K9Periode(
                        LocalDate.now(),
                        LocalDate.now().plusDays(5)
                    ) to TilsynPeriodeInfo(Duration.ofHours(4))
                )
            ),
            Arbeidstid(
                listOf(
                    Arbeidstaker(
                        NorskIdentitetsnummer.of("12345678910"),
                        Organisasjonsnummer.of("926032925"),
                        ArbeidstidInfo(
                            Duration.ofHours(8),
                            mapOf(
                                K9Periode(
                                    LocalDate.now().minusDays(10),
                                    LocalDate.now()
                                ) to ArbeidstidPeriodeInfo(Duration.ofHours(4)),
                                K9Periode(
                                    LocalDate.now().minusDays(20),
                                    LocalDate.now().minusDays(10)
                                ) to ArbeidstidPeriodeInfo(Duration.ofHours(2))
                            )
                        )
                    )
                ),
                null, null
            ),
            Uttak(
                mapOf(
                    K9Periode(
                        LocalDate.now().minusDays(10),
                        LocalDate.now()
                    ) to UttakPeriodeInfo(Duration.ofHours(4)),
                    K9Periode(
                        LocalDate.now().minusDays(20),
                        LocalDate.now().minusDays(10)
                    ) to UttakPeriodeInfo(Duration.ofHours(2))
                )
            ),
            LovbestemtFerie(
                listOf(
                    K9Periode(LocalDate.now().minusDays(10), LocalDate.now()),
                    K9Periode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(10))
                )
            ),
            Bosteder(
                mapOf(
                    K9Periode(
                        LocalDate.now().minusDays(10),
                        LocalDate.now()
                    ) to Bosteder.BostedPeriodeInfo.builder()
                        .land(Landkode.SPANIA)
                        .build(),
                    K9Periode(
                        LocalDate.now().minusDays(20),
                        LocalDate.now().minusDays(10)
                    ) to Bosteder.BostedPeriodeInfo.builder()
                        .land(Landkode.NORGE)
                        .build()
                )
            ),
            K9Utenlandsopphold(
                mapOf(
                    K9Periode(
                        LocalDate.now().minusDays(10),
                        LocalDate.now()
                    ) to UtenlandsoppholdPeriodeInfo.builder()
                        .land(Landkode.CANADA)
                        .årsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
                        .build(),
                    K9Periode(
                        LocalDate.now().minusDays(20),
                        LocalDate.now().minusDays(10)
                    ) to UtenlandsoppholdPeriodeInfo.builder()
                        .land(Landkode.SVERIGE)
                        .årsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING)
                        .build()
                )
            )
        )
    )

    val defaultMeldingV2 = MeldingV2(
        søknad = defaultK9FormatPSB,
        vedleggUrls = listOf(URI("http://localhost:8080/vedlegg/11")),
        interInfo = InternInfo(
            internSøker = InternSøker(
                fornavn = "Ole",
                etternavn = "Hansen",
                aktørId = AktoerId("123456789")
            )
        )
    )
}

internal fun MeldingV1.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
