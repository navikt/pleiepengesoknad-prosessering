package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.Arbeidsgiver
import no.nav.helse.prosessering.v1.MeldingV1
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
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap.BeredskapPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.DataBruktTilUtledning
import no.nav.k9.søknad.ytelse.psb.v1.LovbestemtFerie
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk.NattevåkPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.Omsorg
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import java.math.BigDecimal
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
            fødselsnummer = "02119970078",
            aktørId = "11111111111"
        ),
        vedleggId = listOf("123", "456"),
        opplastetIdVedleggId = listOf("789"),
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
            harInntektSomFrilanser = true,
            startdato = LocalDate.parse("2019-01-01"),
            jobberFortsattSomFrilans = true,
            arbeidsforhold = Arbeidsforhold(
                normalarbeidstid = NormalArbeidstid(
                    erLiktHverUke = true,
                    timerPerUkeISnitt = Duration.ofHours(37).plusMinutes(30)
                ),
                arbeidIPeriode = ArbeidIPeriode(
                    type = ArbeidIPeriodeType.ARBEIDER_VANLIG,
                    arbeiderIPerioden = ArbeiderIPeriodenSvar.SOM_VANLIG
                )
            )
        ),
        selvstendigNæringsdrivende = SelvstendigNæringsdrivende(
            harInntektSomSelvstendig = true,
            virksomhet = Virksomhet(
                næringstype = Næringstyper.ANNEN,
                fraOgMed = LocalDate.parse("2021-01-01"),
                tilOgMed = LocalDate.parse("2021-01-10"),
                navnPåVirksomheten = "Kjells Møbelsnekkeri",
                registrertINorge = true,
                yrkesaktivSisteTreFerdigliknedeÅrene = YrkesaktivSisteTreFerdigliknedeÅrene(LocalDate.parse("2021-01-01")),
                organisasjonsnummer = "111111"
            ),
            arbeidsforhold = Arbeidsforhold(
                normalarbeidstid = NormalArbeidstid(
                    erLiktHverUke = true,
                    timerPerUkeISnitt = Duration.ofHours(37).plusMinutes(30)
                ),
                arbeidIPeriode = ArbeidIPeriode(
                    type = ArbeidIPeriodeType.ARBEIDER_VANLIG,
                    arbeiderIPerioden = ArbeiderIPeriodenSvar.SOM_VANLIG
                )
            )
        ),
        arbeidsgivere = listOf(
            Arbeidsgiver(
                navn = "Peppes",
                organisasjonsnummer = "917755736",
                erAnsatt = true,
                arbeidsforhold = Arbeidsforhold(
                    normalarbeidstid = NormalArbeidstid(
                        erLiktHverUke = true,
                        timerPerUkeISnitt = Duration.ofHours(37).plusMinutes(30)
                    ),
                    arbeidIPeriode = ArbeidIPeriode(
                        type = ArbeidIPeriodeType.ARBEIDER_VANLIG,
                        arbeiderIPerioden = ArbeiderIPeriodenSvar.SOM_VANLIG
                    )
                )
            ),
            Arbeidsgiver(
                navn = "Pizzabakeren",
                organisasjonsnummer = "917755736",
                erAnsatt = true,
                arbeidsforhold = Arbeidsforhold(
                    normalarbeidstid = NormalArbeidstid(
                        erLiktHverUke = true,
                        timerPerUkeISnitt = Duration.ofHours(37).plusMinutes(30)
                    ),
                    arbeidIPeriode = ArbeidIPeriode(
                        type = ArbeidIPeriodeType.ARBEIDER_VANLIG,
                        arbeiderIPerioden = ArbeiderIPeriodenSvar.SOM_VANLIG
                    )
                )
            )
        ),
        harVærtEllerErVernepliktig = true,
        k9FormatSøknad = defaultK9FormatPSB(),
        samtidigHjemme = null,
        omsorgstilbud = null,
        barnRelasjon = null,
        barnRelasjonBeskrivelse = null,
        utenlandskNæring = listOf(
            UtenlandskNæring(
                næringstype = Næringstyper.DAGMAMMA,
                navnPåVirksomheten = "Dagmamma AS",
                land = Land(landkode = "NDL", landnavn = "Nederland"),
                fraOgMed = LocalDate.parse("2020-01-01")
            )
        ),
        opptjeningIUtlandet = listOf(
            OpptjeningIUtlandet(
                navn = "Yolo AS",
                opptjeningType = OpptjeningType.ARBEIDSTAKER,
                land = Land(landkode = "NDL", landnavn = "Nederland"),
                fraOgMed = LocalDate.parse("2020-01-01"),
                tilOgMed = LocalDate.parse("2020-10-01")
            )
        )
    )

    fun defaultK9FormatPSB(søknadId: UUID = UUID.randomUUID()) = Søknad(
        SøknadId.of(søknadId.toString()),
        Versjon.of("1.0.0"),
        ZonedDateTime.parse("2020-01-01T10:00:00Z"),
        K9Søker(NorskIdentitetsnummer.of("12345678910")),
        PleiepengerSyktBarn()
            .medSøknadsperiode(K9Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-10")))
            .medSøknadInfo(DataBruktTilUtledning(true, true, true, true, true))
            .medBarn(K9Barn().medNorskIdentitetsnummer(NorskIdentitetsnummer.of("10987654321")))
            .medOpptjeningAktivitet(
                OpptjeningAktivitet()
                    .medSelvstendigNæringsdrivende(
                        listOf(
                            SelvstendigNæringsdrivende()
                                .medOrganisasjonsnummer(Organisasjonsnummer.of("12345678910112233444455667"))
                                .medVirksomhetNavn("Mamsen Bamsen AS")
                                .medPerioder(
                                    mapOf(
                                        K9Periode(
                                            LocalDate.parse("2018-01-01"),
                                            LocalDate.parse("2020-01-01")
                                        ) to SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo()
                                            .medErNyoppstartet(true)
                                            .medRegistrertIUtlandet(false)
                                            .medBruttoInntekt(BigDecimal(5_000_000))
                                            .medErVarigEndring(true)
                                            .medEndringDato(LocalDate.parse("2020-01-01"))
                                            .medEndringBegrunnelse("Grunnet Covid-19")
                                            .medLandkode(Landkode.NORGE)
                                            .medRegnskapsførerNavn("Regnskapsfører Svensen")
                                            .medRegnskapsførerTlf("+4799887766")
                                            .medVirksomhetstyper(listOf(VirksomhetType.DAGMAMMA, VirksomhetType.ANNEN))
                                    )
                                ),
                            SelvstendigNæringsdrivende()
                                .medOrganisasjonsnummer(Organisasjonsnummer.of("54549049090490498048940940"))
                                .medVirksomhetNavn("Something Fishy AS")
                                .medPerioder(
                                    mapOf(
                                        K9Periode(
                                            LocalDate.parse("2015-01-01"),
                                            LocalDate.parse("2017-01-01")
                                        ) to SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo()
                                            .medErNyoppstartet(false)
                                            .medRegistrertIUtlandet(true)
                                            .medBruttoInntekt(BigDecimal(500_000))
                                            .medErVarigEndring(false)
                                            .medLandkode(Landkode.SPANIA)
                                            .medVirksomhetstyper(listOf(VirksomhetType.FISKE))
                                    )
                                )
                        )
                    )
                    .medFrilanser(Frilanser(LocalDate.parse("2020-01-01"), null))
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
                        Arbeidstaker()
                            .medNorskIdentitetsnummer(NorskIdentitetsnummer.of("12345678910"))
                            .medOrganisasjonsnummer(Organisasjonsnummer.of("926032925"))
                            .medArbeidstidInfo(ArbeidstidInfo()
                                .medPerioder(
                                    mapOf(
                                        K9Periode(
                                            LocalDate.parse("2018-01-01"),
                                            LocalDate.parse("2020-01-05")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(4)),
                                        K9Periode(
                                            LocalDate.parse("2020-01-06"),
                                            LocalDate.parse("2020-01-10")
                                        ) to ArbeidstidPeriodeInfo()
                                            .medJobberNormaltTimerPerDag(Duration.ofHours(8))
                                            .medFaktiskArbeidTimerPerDag(Duration.ofHours(2))
                                    )
                                ))
                    )
                )
            )
            .medUttak(
                Uttak().medPerioder(
                    mapOf(
                        K9Periode(
                            LocalDate.parse("2020-01-01"),
                            LocalDate.parse("2020-01-05")
                        ) to Uttak.UttakPeriodeInfo(Duration.ofHours(4)),
                        K9Periode(
                            LocalDate.parse("2020-01-06"),
                            LocalDate.parse("2020-01-10")
                        ) to Uttak.UttakPeriodeInfo(Duration.ofHours(2))
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
                        ) to UtenlandsoppholdPeriodeInfo()
                            .medLand(Landkode.CANADA)
                            .medÅrsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD),
                        K9Periode(
                            LocalDate.parse("2020-01-06"),
                            LocalDate.parse("2020-01-10")
                        ) to UtenlandsoppholdPeriodeInfo()
                            .medLand(Landkode.SVERIGE)
                            .medÅrsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING)
                    )
                )
            )
    )
}

internal fun MeldingV1.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
