package sablewatch.tools;

import java.io.File;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;

/** Validates a watch face XML file against an XSD 1.1 schema. Used by tools/validate-watchface.sh. */
public final class WffSchemaCheck {
    private static final Logger LOG = Logger.getLogger(WffSchemaCheck.class.getName());
    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private WffSchemaCheck() {}

    public static void main(String[] args) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(DISALLOW_DOCTYPE, true);
        Schema schema = factory.newSchema(new File(args[0]));

        // Parse the watch face with the JDK's own parser, with DTDs refused, so no external entity
        // can be read; then validate the parsed tree.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(DISALLOW_DOCTYPE, true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        Document doc = dbf.newDocumentBuilder().parse(new File(args[1]));

        Validator validator = schema.newValidator();
        validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        validator.validate(new DOMSource(doc, args[1]));
        LOG.info(() -> "Valid Watch Face Format: " + args[1]);
    }
}
