package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.EndringsmeldingUtils.objectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.felles.*
import no.nav.helse.prosessering.v1.asynkron.endringsmelding.EndringsmeldingV1
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.DataBruktTilUtledning
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9.søknad.ytelse.psb.v1.UttakPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import no.nav.k9.søknad.felles.personopplysninger.Barn as K9Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker as K9Søker
import no.nav.k9.søknad.felles.type.Periode as K9Periode
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning as K9Tilsynsordning

internal object EndringsmeldingUtils {
    internal val objectMapper = jacksonObjectMapper().dusseldorfConfigured()
    private val start = LocalDate.parse("2020-01-01")
    private const val GYLDIG_ORGNR = "917755736"

    internal fun defaultEndringsmelding(søknadsId: UUID) = EndringsmeldingV1(
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = "02119970078",
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        k9Format = defaultK9FormatPSB(søknadsId)
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
    )
}

internal fun EndringsmeldingV1.somJson() = objectMapper.writeValueAsString(this)
