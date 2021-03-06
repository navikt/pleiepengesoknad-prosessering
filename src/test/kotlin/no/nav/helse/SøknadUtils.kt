package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.*
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.opptjening.Frilanser
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdPeriodeInfo
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.felles.type.VirksomhetType
import no.nav.k9.søknad.ytelse.psb.v1.*
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap.BeredskapPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.NattevåkPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker
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
        fraOgMed = LocalDate.parse("2021-01-01"),
        tilOgMed = LocalDate.parse("2021-01-01"),
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = "02119970078",
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        barn = Barn(
            navn = "Ole Dole",
            fødselsnummer = "02119970078"
        ),
        arbeidsgivere = Arbeidsgivere(
            organisasjoner = listOf(
                Organisasjon(
                    "917755736",
                    "Gyldig",
                    jobberNormaltTimer = 4.0,
                    skalJobbeProsent = 50.0,
                    skalJobbe = SkalJobbe.REDUSERT,
                    arbeidsform = Arbeidsform.VARIERENDE
                ),
                Organisasjon(
                    "917755734",
                    "Gyldig",
                    jobberNormaltTimer = 40.0,
                    skalJobbeProsent = 40.0,
                    skalJobbe = SkalJobbe.JA,
                    arbeidsform = Arbeidsform.VARIERENDE
                ),
                Organisasjon(
                    "917755734",
                    "Gyldig",
                    jobberNormaltTimer = 8.0,
                    skalJobbeProsent = 0.0,
                    skalJobbe = SkalJobbe.NEI,
                    arbeidsform = Arbeidsform.VARIERENDE
                ),
                Organisasjon(
                    "917755734",
                    "Gyldig",
                    jobberNormaltTimer = 40.0,
                    skalJobbeProsent = 40.0,
                    skalJobbe = SkalJobbe.VET_IKKE,
                    arbeidsform = Arbeidsform.VARIERENDE
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
            startdato = LocalDate.parse("2019-01-01"),
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
        ),
        harVærtEllerErVernepliktig = true,
        k9FormatSøknad = defaultK9FormatPSB()
    )

    fun defaultK9FormatPSB(søknadId: UUID = UUID.randomUUID()) = Søknad(
        SøknadId.of(søknadId.toString()),
        Versjon.of("1.0.0"),
        ZonedDateTime.parse("2020-01-01T10:00:00Z"),
        K9Søker(NorskIdentitetsnummer.of("12345678910")),
        PleiepengerSyktBarn()
            .medSøknadsperiode(K9Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")))
            .medSøknadInfo(DataBruktTilUtledning(true, true, true, true, true))
            .medBarn(K9Barn(NorskIdentitetsnummer.of("10987654321"), null))
            .medOpptjeningAktivitet(
                OpptjeningAktivitet(
                    null,
                    listOf(
                        SelvstendigNæringsdrivende(
                            mapOf(
                                K9Periode(
                                    LocalDate.parse("2018-01-01"),
                                    LocalDate.parse("2020-01-01")
                                ) to SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
                                    .erNyoppstartet(true)
                                    .registrertIUtlandet(false)
                                    .bruttoInntekt(BigDecimal(5_000_000))
                                    .erVarigEndring(true)
                                    .endringDato(LocalDate.parse("2020-01-01"))
                                    .endringBegrunnelse("Grunnet Covid-19")
                                    .landkode(Landkode.NORGE)
                                    .regnskapsførerNavn("Regnskapsfører Svensen")
                                    .regnskapsførerTelefon("+4799887766")
                                    .virksomhetstyper(listOf(VirksomhetType.DAGMAMMA, VirksomhetType.ANNEN))
                                    .build()
                            ),
                            Organisasjonsnummer.of("12345678910112233444455667"),
                            "Mamsen Bamsen AS"
                        ),
                        SelvstendigNæringsdrivende(
                            mapOf(
                                K9Periode(
                                    LocalDate.parse("2015-01-01"),
                                    LocalDate.parse("2017-01-01")
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
                                    .virksomhetstyper(listOf(VirksomhetType.FISKE))
                                    .build()
                            ),
                            Organisasjonsnummer.of("54549049090490498048940940"),
                            "Something Fishy AS"
                        ),
                    ),
                    Frilanser(LocalDate.parse("2020-01-01"), null, true)
                )
            )
            .medBeredskap(
                K9Beredskap().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to BeredskapPeriodeInfo().medTilleggsinformasjon("Jeg skal være i beredskap. Basta!"),
                        K9Periode(
                            LocalDate.parse("2020-01-07"),
                            LocalDate.parse("2020-01-10")
                        ) to BeredskapPeriodeInfo().medTilleggsinformasjon("Jeg skal være i beredskap i denne perioden også. Basta!")
                    )
                )
            )
            .medNattevåk(
                K9Nattevåk().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to NattevåkPeriodeInfo().medTilleggsinformasjon("Jeg skal ha nattevåk. Basta!"),
                        K9Periode(
                            LocalDate.parse("2020-01-07"),
                            LocalDate.parse("2020-01-10")
                        ) to NattevåkPeriodeInfo().medTilleggsinformasjon("Jeg skal ha nattevåk i perioden også. Basta!")
                    )
                )
            )
            .medTilsynsordning(
                K9Tilsynsordning().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to TilsynPeriodeInfo().medEtablertTilsynTimerPerDag(Duration.ofHours(8)),
                        K9Periode(
                            LocalDate.parse("2020-01-06"),
                            LocalDate.parse("2020-01-10")
                        ) to TilsynPeriodeInfo().medEtablertTilsynTimerPerDag(Duration.ofHours(4))
                    )
                )
            )
            .medArbeidstid(
                Arbeidstid().medArbeidstaker(
                    listOf(
                        Arbeidstaker(
                            NorskIdentitetsnummer.of("12345678910"),
                            Organisasjonsnummer.of("926032925"),
                            ArbeidstidInfo(
                                mapOf(
                                    K9Periode(
                                        LocalDate.parse("2018-01-01"),
                                        LocalDate.parse("2020-01-05")
                                    ) to ArbeidstidPeriodeInfo(Duration.ofHours(8), Duration.ofHours(4)),
                                    K9Periode(
                                        LocalDate.parse("2020-01-06"),
                                        LocalDate.parse("2020-01-10")
                                    ) to ArbeidstidPeriodeInfo(Duration.ofHours(8), Duration.ofHours(2))
                                )
                            )
                        )
                    )
                )
            )
            .medUttak(
                Uttak().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to UttakPeriodeInfo(Duration.ofHours(4)),
                        K9Periode(
                            LocalDate.parse("2020-01-06"),
                            LocalDate.parse("2020-01-10")
                        ) to UttakPeriodeInfo(Duration.ofHours(2))
                    )
                )
            )
            .medOmsorg(
                Omsorg()
                    .medRelasjonTilBarnet(Omsorg.BarnRelasjon.MOR)
                    .medBeskrivelseAvOmsorgsrollen("Blabla beskrivelse")
            )
            .medLovbestemtFerie(
                LovbestemtFerie().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to LovbestemtFerie.LovbestemtFeriePeriodeInfo(),
                        K9Periode(
                            LocalDate.parse("2020-01-06"),
                            LocalDate.parse("2020-01-10")
                        ) to LovbestemtFerie.LovbestemtFeriePeriodeInfo()
                    )
                )
            )
            .medBosteder(
                Bosteder().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to Bosteder.BostedPeriodeInfo().medLand(Landkode.SPANIA),
                        K9Periode(
                            LocalDate.parse("2020-01-06"),
                            LocalDate.parse("2020-01-10")
                        ) to Bosteder.BostedPeriodeInfo().medLand(Landkode.NORGE)
                    )
                )
            )
            .medUtenlandsopphold(
                K9Utenlandsopphold().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to UtenlandsoppholdPeriodeInfo.builder()
                            .land(Landkode.CANADA)
                            .årsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
                            .build(),
                        K9Periode(
                            LocalDate.parse("2020-01-06"),
                            LocalDate.parse("2020-01-10")
                        ) to UtenlandsoppholdPeriodeInfo.builder()
                            .land(Landkode.SVERIGE)
                            .årsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING)
                            .build()
                    )
                )
            )
    )
}

internal fun MeldingV1.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)