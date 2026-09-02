package com.empresa.actas.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    private MockHttpServletRequest peticion(String metodo, String ruta, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest(metodo, ruta);
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void loginSobreElLimiteResponde429() throws Exception {
        RateLimitFilter filtro = new RateLimitFilter();
        ReflectionTestUtils.setField(filtro, "loginMax", 5);
        ReflectionTestUtils.setField(filtro, "loginSegundos", 60);

        int excesos = 0;
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filtro.doFilter(peticion("POST", "/auth/login", "192.168.1.99"),
                    res, new MockFilterChain());
            if (res.getStatus() == 429) {
                excesos++;
            }
        }
        assertEquals(5, excesos);
    }

    @Test
    void loginDeIpDistintaNoSeVeAfectado() throws Exception {
        RateLimitFilter filtro = new RateLimitFilter();
        ReflectionTestUtils.setField(filtro, "loginMax", 2);
        ReflectionTestUtils.setField(filtro, "loginSegundos", 60);

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            // Cada peticion desde una IP distinta: nunca supera el limite por IP.
            filtro.doFilter(peticion("POST", "/auth/login", "192.168.1." + (10 + i)),
                    res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    void registroSeLimitaIndependienteDelLogin() throws Exception {
        RateLimitFilter filtro = new RateLimitFilter();
        ReflectionTestUtils.setField(filtro, "registroMax", 3);
        ReflectionTestUtils.setField(filtro, "registroSegundos", 60);

        int excesos = 0;
        for (int i = 0; i < 6; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filtro.doFilter(peticion("POST", "/auth/register", "192.168.1.99"),
                    res, new MockFilterChain());
            if (res.getStatus() == 429) {
                excesos++;
            }
        }
        assertEquals(3, excesos);
    }
}