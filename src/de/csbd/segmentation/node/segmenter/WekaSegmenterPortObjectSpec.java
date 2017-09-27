package de.csbd.segmentation.node.segmenter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

public class WekaSegmenterPortObjectSpec extends AbstractSimplePortObjectSpec {

    public static final class Serializer extends AbstractSimplePortObjectSpecSerializer<WekaSegmenterPortObjectSpec> {
    }

    public WekaSegmenterPortObjectSpec() {
    }


    @Override
    protected void save(final ModelContentWO model) {
    }

    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
    }
}
