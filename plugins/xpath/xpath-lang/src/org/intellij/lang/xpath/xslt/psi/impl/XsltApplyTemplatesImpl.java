/*
 * Copyright (c) 2005, Your Corporation. All Rights Reserved.
 */

/*
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 14.12.2005
 * Time: 18:32:33
 */
package org.intellij.lang.xpath.xslt.psi.impl;

import org.intellij.lang.xpath.psi.XPathExpression;
import org.intellij.lang.xpath.xslt.psi.XsltApplyTemplates;
import org.intellij.lang.xpath.xslt.util.XsltCodeInsightUtil;

import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class XsltApplyTemplatesImpl extends XsltTemplateInvocationBase implements XsltApplyTemplates {

    protected XsltApplyTemplatesImpl(XmlTag target) {
        super(target);
    }

    @Override
    public String toString() {
        return "XsltApplyTemplates[" + getSelect() + "]";
    }

    @Nullable
    public XPathExpression getSelect() {
        return XsltCodeInsightUtil.getXPathExpression(this, "select");
    }

    public QName getMode() {
        return getQName(getTag().getAttributeValue("mode"));
    }
}