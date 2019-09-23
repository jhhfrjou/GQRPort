import gqr.*;
import org.junit.jupiter.api.*;
import uk.ac.ox.cs.chaseBench.model.DatabaseSchema;
import uk.ac.soton.ecs.RelationalModel.Exceptions.InconsistentAtomException;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GQRTest {
    GQR gqr = new GQR("resources/query_0.txt", "resources/views_for_q_0.txt", -1);


    @Test
    void reformulatePassingPaperQuery() {
        try {
            gqr = new GQR("resources/paper_query.txt", "resources/paper_views.txt", -1);
            List<CompRewriting> rewrites = gqr.reformulate(gqr.getQueries().get(0));
            rewrites.forEach(System.out::println);
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {
            fail();
        }
    }

    @Test
    void reformulatePassingQ55() {
        try {
            gqr = new GQR("resources/queryHD_55.txt", "resources/views_55.txt", -1);
            gqr.reformulate(gqr.getQueries().get(0));
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {
            fail();
        }
    }

    @Test
    void reformulatePassingQ59() {
        try {
            gqr = new GQR("resources/queryHD_59.txt", "resources/views_59.txt", -1);
            gqr.reformulate(gqr.getQueries().get(0));
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {
            fail();
        }
    }


    @Test
    void reformulatePassingQ95() {
        try {
            gqr = new GQR("resources/queryHD_95.txt", "resources/views_95.txt", -1);
            gqr.reformulate(gqr.getQueries().get(0));
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {
            fail();
        }
    }

    @Test
    void reformulateFailPaperQuery() {
        gqr = new GQR("resources/paper_query.txt", "resources/paper_views.txt", 0);
        try {
            gqr.reformulate(gqr.getQueries().get(0));
            assertThrows(NonAnswerableQueryException.class, null);
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {

        }
    }

    @Test
    void reformulateFailQ55() {
        try {
            gqr = new GQR("resources/queryHD_55.txt", "resources/views_55.txt", 1);
            gqr.reformulate(gqr.getQueries().get(0));
            assertThrows(NonAnswerableQueryException.class, null);
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {
        }
    }

    @Test
    void reformulateFailQ59() {
        try {
            gqr = new GQR("resources/queryHD_59.txt", "resources/views_59.txt", 1);
            gqr.reformulate(gqr.getQueries().get(0));
            assertThrows(NonAnswerableQueryException.class, null);
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {
        }
    }

    @Test
    void reformulateFailQ95() {
        try {
            gqr = new GQR("resources/queryHD_95.txt", "resources/views_95.txt", 1);
            gqr.reformulate(gqr.getQueries().get(0));
            assertThrows(NonAnswerableQueryException.class, null);
        } catch (NonAnswerableQueryException | InconsistentAtomException e) {
        }
    }

    @Test
    void writeValidSchema() {
        File source = new File("tempSource.txt");
        File target = new File("tempTarget.txt");
        try {
            uk.ac.ox.cs.chaseBench.model.DatabaseSchema dbSchema = new DatabaseSchema();
            GQR.generateSchema(dbSchema, GQR.readRulesfromFile("resources/paper_views.txt"));
            dbSchema.save(target, true);
            dbSchema.save(source, false);
            uk.ac.ox.cs.chaseBench.model.DatabaseSchema dbSchema2 = new uk.ac.ox.cs.chaseBench.model.DatabaseSchema();

            dbSchema2.load(target, true);
            dbSchema2.load(source, false);
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