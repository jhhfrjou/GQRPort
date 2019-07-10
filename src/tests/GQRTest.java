import gqr.*;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class GQRTest {
    GQR gqr;
    @BeforeEach
    void setUp() {
        gqr = new GQR("resources/query_0.txt","resources/views_for_q_0.txt");

    }

    @Test
    void getQuery() {
        Query query = gqr.getQuery();
        System.out.println(query);
        assertEquals("q0(X0,X1,X6,X2,X7,X8,X4,X11,X15,X17) :- m19004(X0,X1,X5,X6),m4004(X1,X2,X7,X8),m10004(X2,X3,X9,X10),m7004(X3,X4,X11,X12),m14004(X4,X5,X13,X14),m5004(X5,X6,X15,X16),m2004(X6,X7,X17,X18),m17004(X7,X8,X19,X20)", query.toString());
    }

    @Test
    void reformulatePassing() {
        try {
            gqr.reformulate(gqr.getQuery());
            //assertThrows(NonAnswerableQueryException.class,null);
        } catch (NonAnswerableQueryException e) {
            fail();
        }

    }

    @Test
    void reformulateFail() {
        gqr = new GQR("resources/paper_query.txt","resources/paper_views.txt");
        try {
            gqr.reformulate(gqr.getQuery());
            assertThrows(NonAnswerableQueryException.class,null);
        } catch (NonAnswerableQueryException e) {

        }

    }
}