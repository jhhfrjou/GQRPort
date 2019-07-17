import gqr.*;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class GQRTest {
    GQR gqr;


    @Test
    void reformulatePassingPaperQuery() {
        try {
            gqr = new GQR("resources/paper_query.txt","resources/paper_views.txt", -1);
            gqr.reformulate(gqr.getQuery());
        } catch (NonAnswerableQueryException e) {
            fail();
        }
    }

    @Test
    void reformulatePassingQ0() {
        try {
            gqr = new GQR("resources/query_0.txt","resources/views_for_q_0.txt", -1);
            gqr.reformulate(gqr.getQuery());
        } catch (NonAnswerableQueryException e) {
            fail();
        }
    }

    @Test
    void reformulatePassingQ55() {
        try {
            gqr = new GQR("resources/queryHD_55.txt", "resources/views_55.txt", -1);
            gqr.reformulate(gqr.getQuery());
        } catch (NonAnswerableQueryException e) {
            fail();
        }
    }

    @Test
    void reformulatePassingQ59() {
        try {
            gqr = new GQR("resources/queryHD_59.txt", "resources/views_59.txt", -1);
            gqr.reformulate(gqr.getQuery());
        } catch (NonAnswerableQueryException e) {
            fail();
        }
    }


    @Test
    void reformulatePassingQ95() {
        try {
            gqr = new GQR("resources/queryHD_95.txt","resources/views_95.txt", -1);
            gqr.reformulate(gqr.getQuery());
        } catch (NonAnswerableQueryException e) {
            fail();
        }
    }

    @Test
    void reformulateFailPaperQuery() {
        gqr = new GQR("resources/paper_query.txt","resources/paper_views.txt", 0);
        try {
            gqr.reformulate(gqr.getQuery());
            assertThrows(NonAnswerableQueryException.class,null);
        } catch (NonAnswerableQueryException e) {

        }
    }

    @Test
    void reformulateFailQ0() {
        try {
            gqr = new GQR("resources/query_0.txt","resources/views_for_q_0.txt", 1);
            gqr.reformulate(gqr.getQuery());
            //assertThrows(NonAnswerableQueryException.class,null);
        } catch (NonAnswerableQueryException e) {        }
    }


    @Test
    void reformulateFailQ55() {
        try {
            gqr = new GQR("resources/queryHD_55.txt","resources/views_55.txt", 1);
            gqr.reformulate(gqr.getQuery());
            assertThrows(NonAnswerableQueryException.class,null);
        } catch (NonAnswerableQueryException e) {        }
    }
    @Test
    void reformulateFailQ59() {
        try {
            gqr = new GQR("resources/queryHD_59.txt","resources/views_59.txt", 1);
            gqr.reformulate(gqr.getQuery());
            assertThrows(NonAnswerableQueryException.class,null);
        } catch (NonAnswerableQueryException e) {        }
    }
    @Test
    void reformulateFailQ95() {
        try {
            gqr = new GQR("resources/queryHD_95.txt","resources/views_95.txt", 1);
            gqr.reformulate(gqr.getQuery());
            assertThrows(NonAnswerableQueryException.class,null);
        } catch (NonAnswerableQueryException e) {        }
    }

    @Test
    void writeValidSchema() {
        File source = new File("tempSource.txt");
        File target = new File("tempTarget.txt");
        try {
            uk.ac.ox.cs.chaseBench.model.DatabaseSchema dbSchema = GQR.generateSchema(GQR.readRulesfromFile("resources/paper_views.txt"));
            dbSchema.save(target,true);
            dbSchema.save(source,false);
            uk.ac.ox.cs.chaseBench.model.DatabaseSchema dbSchema2 = new uk.ac.ox.cs.chaseBench.model.DatabaseSchema();
            dbSchema2.load(target,true);
            dbSchema2.load(source,false);
            source.delete();
            target.delete();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            source.delete();
            target.delete();
        }

    }
}