import java.io.File;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/** Validates a watch face XML file against an XSD 1.1 schema. Used by tools/validate-watchface.sh. */
public final class WffSchemaCheck {
    public static void main(String[] args) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        Schema schema = factory.newSchema(new File(args[0]));
        schema.newValidator().validate(new StreamSource(new File(args[1])));
        System.out.println("Valid Watch Face Format: " + args[1]);
    }
}
