package com.example.monoco.dominio

import org.junit.Assert.*
import org.junit.Test

class RegrasMonocoTest {
    @Test fun acaoVaziaOuApenasEspacosNaoPodeSerEnviada() {
        assertNotNull(RegrasMonoco.erroAcao(""))
        assertNotNull(RegrasMonoco.erroAcao(" \n\t "))
    }

    @Test fun aceitaExatamenteDoisMilCaracteres() {
        assertNull(RegrasMonoco.erroAcao("a".repeat(2000)))
    }

    @Test fun recusaDoisMilEUmCaracteresSemModificarEntrada() {
        val texto = "a".repeat(2001)
        assertNotNull(RegrasMonoco.erroAcao(texto))
        assertEquals(2001, texto.length)
    }

    @Test fun criatividadeAceitaOsDoisLimites() {
        assertNull(RegrasMonoco.erroCriatividade(0f))
        assertNull(RegrasMonoco.erroCriatividade(1f))
    }

    @Test fun criatividadeRecusaValoresForaDaFaixaENaoFinitos() {
        listOf(-0.1f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            assertNotNull(RegrasMonoco.erroCriatividade(it))
        }
    }

    @Test fun memoriaPrecisaDeTituloEConteudo() {
        assertNotNull(RegrasMonoco.erroMemoria("", "Lia é uma aliada"))
        assertNotNull(RegrasMonoco.erroMemoria("Aliados", "  "))
        assertNull(RegrasMonoco.erroMemoria("Aliados", "Lia é uma aliada"))
    }
}
