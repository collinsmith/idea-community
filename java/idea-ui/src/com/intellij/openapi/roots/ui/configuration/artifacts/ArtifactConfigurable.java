/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.ui.configuration.artifacts;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author nik
 */
public class ArtifactConfigurable extends ArtifactConfigurableBase {
  private final ArtifactEditorImpl myEditor;
  private boolean myIsInUpdateName;

  public ArtifactConfigurable(Artifact originalArtifact, ArtifactsStructureConfigurableContextImpl artifactsStructureContext, final Runnable updateTree) {
    super(originalArtifact, artifactsStructureContext, updateTree, true);
    myEditor = artifactsStructureContext.getOrCreateEditor(originalArtifact);
  }

  public void setDisplayName(String name) {
    final String oldName = getArtifact().getName();
    if (name != null && !name.equals(oldName) && !myIsInUpdateName) {
      myArtifactsStructureContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact).setName(name);
      myEditor.updateOutputPath(oldName, name);
    }
  }

  @Override
  public void updateName() {
    myIsInUpdateName = true;
    try {
      super.updateName();
    }
    finally {
      myIsInUpdateName = false;
    }
  }

  public JComponent createOptionsPanel() {
    return myEditor.createMainComponent();
  }

  @Override
  protected JComponent createTopRightComponent() {
    final ComboBox artifactTypeBox = new ComboBox();
    for (ArtifactType type : ArtifactType.getAllTypes()) {
      artifactTypeBox.addItem(type);
    }

    artifactTypeBox.setRenderer(new ArtifactTypeCellRenderer());

    artifactTypeBox.setSelectedItem(getArtifact().getArtifactType());
    artifactTypeBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final ArtifactType selected = (ArtifactType)artifactTypeBox.getSelectedItem();
        if (selected != null && !Comparing.equal(selected, getArtifact().getArtifactType())) {
          myEditor.setArtifactType(selected);
        }
      }
    });

    final JPanel panel = new JPanel(new FlowLayout());
    panel.add(new JLabel("Type: "));
    panel.add(artifactTypeBox);
    return panel;
  }

  public boolean isModified() {
    return myEditor.isModified();
  }

  public void apply() throws ConfigurationException {
    myEditor.apply();
  }

  public void reset() {
  }

  public String getHelpTopic() {
    return myEditor.getHelpTopic();
  }
}
