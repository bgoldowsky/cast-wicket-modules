/*
 * Copyright 2011 CAST, Inc.
 *
 * This file is part of the CAST Wicket Modules:
 * see <http://code.google.com/p/cast-wicket-modules>.
 *
 * The CAST Wicket Modules are free software: you can redistribute and/or
 * modify them under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The CAST Wicket Modules are distributed in the hope that they will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cast.cwm.data.component;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.cast.cwm.service.EventService;

import com.visural.wicket.component.submitters.impl.ModalCSSRef;

/**
 * A Styled modal dialog popup box.  This provides a default style and markup, but applications
 * can override these.  The title of the dialog box is set by this component's model, while
 * its content is the body of the border.
 * 
 * This is similar to Dialog, but is a border so that it can enforce consistent markup of dialogs.
 * This may replace AbstractStyledDialog at some point since it is more flexible.
 * 
 * @author bgoldowsky
 * 
 * TODO: determine if we should keep styleReferences or use overriding to set styles
 * TODO: somehow make default stylesheet not interfere so much with application stylesheets
 * TODO: remove Visural dependencies
 *
 * @param <T>
 */
public class DialogBorder extends Border implements IHeaderContributor {

	private static final long serialVersionUID = 1L;
	
	private static final ResourceReference JS_REFERENCE = new JavascriptResourceReference(DialogBorder.class, "DialogBorder.js");
	
	@Getter @Setter
	private boolean logEvents = true;
	
	@Getter @Setter
	private String eventCode = "dialog:open";
	
	@Getter @Setter
	private String eventDetail;
	
	@Getter @Setter
	private String pageName;
		
	@Getter @Setter
	protected boolean clickBkgToClose = false;
	
	@Getter @Setter
	protected Integer zIndex;
	
	@Getter @Setter
	protected boolean masking = true;

	@Getter
	protected WebMarkupContainer contentContainer;
	
	/**
	 * The ancestor of this DialogBorder thats client-side markup will be moved, via Javascript, 
	 * to the end of the page.  This can solve some CSS display issues.  If null, the 
	 * contentContainer will be moved instead, possibly separating the client-side markup of 
	 * this dialog from its wicket ancestry.
	 */
	@Getter @Setter
	protected WebMarkupContainer moveContainer;

	@Getter
	protected WebMarkupContainer overlay;
	
	// Holds application specified styles for this border.
	private static List<ResourceReference> styleReferences;
	
	private AbstractDefaultAjaxBehavior openEventBehavior;
	
	
	/**
	 * When the dialog is closed, but the opening button is no longer on the page, the DialogBorder will
	 * attempt to return focus to a component based on this jQuery selector.
	 */
	@Getter @Setter
	protected String focusFallbackSelector;

	public DialogBorder (String id) {
		this(id, "");
	}
	
	public DialogBorder (String id, String title) {
		this(id, new Model<String>(title));
	}
	
	public DialogBorder (String id, final IModel<String> model) {
		super(id, model);
		setRenderBodyOnly(true);
		
		addContentContainer();
		contentContainer.setOutputMarkupId(true);
		contentContainer.add(new SimpleAttributeModifier("tabindex", "-1"));
		
		addTitle(contentContainer);
		addCloseLink(contentContainer);
		addBody(contentContainer);
		
		addOverlay();
		
		openEventBehavior = new AbstractDefaultAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void respond(AjaxRequestTarget target) {
				logOpenEvent(target);
			}
		};
		add(openEventBehavior);
	}
	
	/**
	 * Log an event when the dialog is opened.
	 * 
	 * @param target
	 */
	protected void logOpenEvent(AjaxRequestTarget target) {
		EventService.get().saveEvent(eventCode, eventDetail, pageName);
	}
	
	
	
	/**
	 * Add the border's body.
	 * @param container main content container
	 */
	protected void addBody (WebMarkupContainer container) {
		container.add(getBodyContainer());
	}
	
	/**
	 * Create and add the overall container for the dialog.
	 */
	protected void addContentContainer() {
		add (contentContainer = new WebMarkupContainer("contentContainer") {
		    private static final long serialVersionUID = 1L;
			@Override
		    protected void onComponentTag(ComponentTag tag) {
		    	super.onComponentTag(tag);
		    	tag.put("style", "display:none;" + (zIndex == null ? "" : "z-index: " + zIndex.toString()));
		    }
		});		
	}
	
	/**
	 * Add a title component for the dialog.
	 * @param container
	 */
	protected void addTitle(WebMarkupContainer container) {
		container.add(new Label("title", getModel()));
	}
	
	/**
	 * Add a close link to the dialog.
	 * @param container
	 */
	protected void addCloseLink(WebMarkupContainer container) {
		WebMarkupContainer link = new WebMarkupContainer("closeWindowLink");
		container.add(link);
		link.add(getClickToCloseBehavior());
	}
	
	/**
	 * Create and add the window-masking overlay, if appropriate.
	 */
	protected void addOverlay() {

		overlay = new WebMarkupContainer("overlay") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(DialogBorder.this.masking);
				if (DialogBorder.this.clickBkgToClose)
					add(DialogBorder.this.getClickToCloseBehavior());
			}
		};
		add(overlay);
		overlay.setOutputMarkupId(true);
	}
	
    /**
     * Open using the current Ajax context.
     * @param target
     */
    public void open(AjaxRequestTarget target) {
        target.appendJavascript(getOpenString());
    }

    /**
     * Close using the current Ajax context.
     * @param target
     */
    public void close(AjaxRequestTarget target) {
        target.appendJavascript(getCloseString());
    }

    /**
     * Return a Behavior which when applied to a Component will add an "onclick"
     * event handler that will open this Dialog.
     * @return
     */
    public IBehavior getClickToOpenBehavior() {
        return new AbstractBehavior() {
        	
			private static final long serialVersionUID = 1L;

			@Override
            public void onComponentTag(Component component, ComponentTag tag) {
                tag.put("onclick", getOpenString());
            }
        };
    }

    /**
     * Return a Behavior which when applied to a Component will add an "onlick"
     * event handler that will close this Dialog.
     * @return
     */
    public IBehavior getClickToCloseBehavior() {
        return new AbstractBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
            public void onComponentTag(Component component, ComponentTag tag) {
                tag.put("onclick", getCloseString());
            }
        };
    }

    /**
     * Return Javascript to run to open the dialog.
     * @return
     */
    public String getOpenString() {
        return getOpenString(true);
    }
    
    public String getOpenString(boolean storeCallingButton) {
    	if (!isEnabled()) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        if (masking) {
        	result.append("$('#" + overlay.getMarkupId() + "').show();");
        	result.append("$('#" + overlay.getMarkupId() + "').height($(document).height()); ");
        }
        
        result.append("$('#" + contentContainer.getMarkupId() + "').show();");
        result.append(getPositioningString());
        result.append("DialogBorder.focusDialog('" + contentContainer.getMarkupId() + "', " + (storeCallingButton? "true" : "false") + ");");
        if (logEvents)
        	result.append("wicketAjaxGet('" + openEventBehavior.getCallbackUrl() + "');");
        return result.toString();	
    }
    
    /**
     * Return the Javascript that will position the dialog when it is opened.
     * By default centers the dialog in the window, but you can override this
     * method to change the behavior.
     * @return String of Javascript.
     */
    protected String getPositioningString() {
    	return "$('#" + contentContainer.getMarkupId() + "').center(); ";
    }

    
    /**
     * Return javascript that will be run when the dialog is closed.
     * @return
     */
    public String getCloseString() {
    	return getCloseString(true);
    }

    /**
     * Return javascript that will be run when the dialog is closed.  Optional 
     * parameter lets you prevent this method from changing focus.  This can be
     * useful if you just want your dialog to disappear silently (e.g. a loading
     * dialog).  If you do not adjust focus here, you should adjust focus elsewhere
     * to maintain accessibility.
     * 
     * @param adjustFocus
     * @return
     */
    public String getCloseString(boolean returnFocus) {
    	StringBuffer result = new StringBuffer();
        result.append("$('#" + contentContainer.getMarkupId() + "').hide();");
        if (masking)
        	result.append("$('#" + overlay.getMarkupId() + "').hide();");
        if (returnFocus)
        	result.append("DialogBorder.focusButton(" + (focusFallbackSelector == null ?  "" : "'" + focusFallbackSelector + "'") + ");");
        return result.toString();
    }

	public void renderHead(final IHeaderResponse response) {
        
		response.renderJavascriptReference(JS_REFERENCE);
        response.renderCSSReference(new ModalCSSRef());

        // Move dialog components out of any containers on the page so that page CSS doesn't interfere with proper display
        // and so that they display on top of other content in older IE versions that don't respect z-index.
        if (moveContainer != null && moveContainer.contains(this, true)) {
        	moveContainer.setOutputMarkupId(true);
        	response.renderOnDomReadyJavascript("$('#" + moveContainer.getMarkupId() + "').detach().appendTo('body');");
        } else {
        	response.renderOnDomReadyJavascript(
				"$('#" + contentContainer.getMarkupId() + "').detach().appendTo('body');"
				+ (masking ? "$('#" + overlay.getMarkupId() + "').detach().appendTo('body');" : ""));
        }
		if (styleReferences == null)
			response.renderCSSReference(new ResourceReference(DialogBorder.class, "modal.css"));
		else
			for (ResourceReference ref : styleReferences)
				response.renderCSSReference(ref);
	}

	/**
	 * Set the list of CSS style resource references that this dialog should
	 * render.  Use this to override the default styles.
	 * 
	 * @param list
	 */
	public static void setStyleReferences(List<ResourceReference> list) {
		styleReferences = list;
	}

	/**
	 * Gets model.  The model of a DialogBorder is used to display the title.
	 * 
	 * @return model
	 */
	@SuppressWarnings("unchecked")
	public final IModel<String> getModel()
	{
		return (IModel<String>)getDefaultModel();
	}

	/**
	 * Sets model
	 * 
	 * @param model
	 */
	public final void setModel(IModel<String> model)
	{
		setDefaultModel(model);
	}

	/**
	 * Gets model object
	 * 
	 * @return model object
	 */
	public final String getModelObject()
	{
		return getModel().getObject();
	}

	/**
	 * Sets model object
	 * 
	 * @param object
	 */
	public final void setModelObject(String title)
	{
		setDefaultModelObject(title);
	}

}