/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package de.csbd.segmentation.node.skeleton;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Empty skeleton for a KNIME {@link NodeModel}
 * 
 * @author Tim-Oliver Buchholz, University of Konstanz
 */

// TODO I'd add another example for how to use the streaming API as well. Maybe in another node model (just for future reference).
// TODO I'd add another empty node model which uses other PortObjects as well (just for future reference)
// TODO I'd add a very simple example node model which e.g. just copies an image was well. Makes it easier to understand.
public class SkeletonNodeModel extends NodeModel {

	// The logger instance
	private static final NodeLogger LOGGER = NodeLogger.getLogger(SkeletonNodeModel.class);

	/**
	 * Constructor of the SkeletonNodeModel for the Table -> Table case.
	 */
	protected SkeletonNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
		// called before node execution (e.g. when a node connection is made or
		// dialog settings are applied). No data available.
		return new DataTableSpec[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		// called on node execution. Has usually no side-effects. Side-effects
		// can be cleared e.g. on reset() or by override onDispose
		return new BufferedDataTable[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// Save the current setting to the NodeSettingsModel.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		// Validate the stored settings.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		// Load the validated settings from the NodeSettingsModel.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// Load internals into the derived NodeModel. This method is only called
		// if the Node was executed. Read all your internal structures from the
		// given file directory to create your internal data structure which is
		// necessary to provide all node functionalities after the workflow is
		// loaded, e.g. view content and/or hilite mapping.

		// Ex.: data used after node execution to restore node state.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// Save internals of the derived NodeModel. This method is only called
		// if the Node is executed. Write all your internal structures into the
		// given file directory which are necessary to recreate this model when
		// the workflow is loaded, e.g. view content and/or hilite mapping.

		// Ex.: data used after node execution to restore node state
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Reset your NodeModel. All components should unregister themselves
		// from any observables (at least from the hilite handler right now).

		// Ex.: stateful variables set during execution
	}

}
