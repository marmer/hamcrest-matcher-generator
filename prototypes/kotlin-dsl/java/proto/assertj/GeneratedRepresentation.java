package proto.assertj;

import org.assertj.core.presentation.StandardRepresentation;
import proto.model.OrderLine;

/** PROTOTYPE. Candidate: a generated AssertJ Representation for generated types without toString. */
public class GeneratedRepresentation extends StandardRepresentation {
    @Override
    protected String fallbackToStringOf(Object o) {
        if (o instanceof OrderLine l) {
            return "OrderLine {sku=" + toStringOf(l.getSku()) + ", quantity=" + toStringOf(l.getQuantity()) + "}";
        }
        return super.fallbackToStringOf(o);
    }
}
