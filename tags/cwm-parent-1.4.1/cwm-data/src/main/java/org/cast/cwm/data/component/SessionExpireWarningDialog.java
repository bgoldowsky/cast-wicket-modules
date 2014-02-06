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

import lombok.Getter;
import net.databinder.auth.AuthDataSessionBase;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.cast.cwm.CwmApplication;
import org.cast.cwm.CwmSession;
import org.cast.cwm.service.ICwmService;
import org.cast.cwm.service.IEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Add this to any page that should warn the user when their session is going to expire.  This
 * will function improperly if the application does not reset the counter upon every 
 * Ajax request (see {@link #getResetJavascript()}).
 * 
 * @author jbrookover
 *
 */
public class SessionExpireWarningDialog extends Panel implements IHeaderContributor {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(SessionExpireWarningDialog.class);
	
	public static final ResourceReference JAVASCRIPT_REFERENCE = new ResourceReference(SessionExpireWarningDialog.class, "SessionExpireWarningDialog.js");
	
	@Inject
	private ICwmService cwmService;
	
	@Inject
	private IEventService eventService;

	@Getter
	private int warningTime = 60 * 5; // Number of seconds before session expires that the user receives a warning.
	
	@Getter
	private int responseTime = 60 * 4; // Number of seconds after warning before the user is automatically logged out.
	
	private AbstractDefaultAjaxBehavior inactiveBehavior;
	
	@Getter
	private DialogBorder dialogBorder;
	
	public SessionExpireWarningDialog(String id) {
		super(id);
		
		addDialog();
		
		inactiveBehavior = new AbstractDefaultAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void respond(AjaxRequestTarget target) {
				onInactive(target);
			}
		};
		add(inactiveBehavior);
	}
	
	protected void addDialog() {
		dialogBorder = new DialogBorder ("dialogBorder", "Session is about to expire") {
			private static final long serialVersionUID = 1L;
			@Override
			protected void addCloseLink(WebMarkupContainer container) {
				super.addCloseLink(container);
				container.get("closeWindowLink").setVisible(false);
			}
		};
		dialogBorder.setLogEvents(false); // Don't log opening of this dialog, or the AJAX activity will prevent logout indefinitely.
		dialogBorder.setZIndex(6000); // This should appear above other dialogs, which default to 5500
		dialogBorder.setMoveContainer(this); // Move entire Panel to end of page.
		add(dialogBorder);
		
		dialogBorder.add(new AjaxFallbackLink<Void>("keepAliveLink") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				keepAliveCall(target);
				dialogBorder.close(target);
			}
		});	
		
	}
	
	/**
	 * Called when the user indicates that they are still working.  By default,
	 * does nothing.
	 * 
	 * @param target
	 */
	protected void keepAliveCall(AjaxRequestTarget target) {
		/* Nothing by default */
	}
	
	/**
	 * <p>
	 * Called when the user did not respond to warning in the designated time.  This
	 * is where you can logout and redirect.  By default, a logged in user is redirected
	 * to {@link CwmApplication#getSignInPageClass()} with parameter 'expired=true' and 
	 * all others are directed to the homepage.
	 * </p>
	 * 
	 * <strong>Note</strong>: In a proper setup, this call itself will refresh
	 * the HttpSession.  So, if no action is taken, the mere presence of this component
	 * will keep the session alive.
	 * 
	 * TODO: Make forceCloseLoginSession and CwmSession.get().signOut() event logging more flexible,
	 * now that we can preemptively logout users.
	 * 
	 * @param target
	 */
	protected void onInactive(AjaxRequestTarget target) {
		
		log.info("Inactive User Detected");
		
		// We're signed in; redirect to login.
		if (CwmSession.get().getLoginSession() != null) {
			eventService.forceCloseLoginSession(CwmSession.get().getLoginSession(), "[timeout]");
			cwmService.flushChanges();
			CwmSession.get().setLoginSessionModel(null);
			AuthDataSessionBase.get().signOut();
			setResponsePage(CwmApplication.get().getSignInPageClass(), new PageParameters("expired=true"));
			setRedirect(true);
		
		// We're not signed in; redirect to home because page is no longer functional
		} else {
			setResponsePage(CwmApplication.get().getHomePage());
			setRedirect(true);
		}			
	}

	public void renderHead(IHeaderResponse response) {
		if (warningTime <= responseTime) {
			throw new IllegalStateException("Warning time must be greater than response time.");
		}

		if (warningTime >= CwmApplication.get().getSessionTimeout()) {
			throw new IllegalStateException("Warning time must be less than session time.");
		}

		response.renderJavascriptReference(JAVASCRIPT_REFERENCE);
		StringBuffer script = new StringBuffer();
		script.append("SessionExpireWarning.init(");
		script.append(CwmApplication.get().getSessionTimeout() + ", ");
		script.append(warningTime + ", ");
		script.append("function() {" + dialogBorder.getOpenString() + "}, ");
		script.append(responseTime + ", ");
		script.append("function() { ");
		script.append(getBeforeInactiveTimeoutJavaScript());
		script.append("wicketAjaxGet('" + inactiveBehavior.getCallbackUrl() + "'); ");
		script.append("}");
		script.append(");"); 
		
		response.renderOnDomReadyJavascript(script.toString());
	}
	
	protected String getBeforeInactiveTimeoutJavaScript() {
		// Subclasses can use this to execute JavaScript just before the seeion close callback.
		return "";
	}
	/**
	 * <p>
	 * Returns a javascript that will reset the counter.  Append this to every AjaxRequestTarget by 
	 * overriding the following method in WebApplication:
	 * </p>
	 * 
	 * <pre>
	 * &#64;Override
	 * public AjaxRequestTarget newAjaxRequestTarget(final Page page) {
	 *     AjaxRequestTarget target = new AjaxRequestTarget(page);
	 *     target.appendJavascript(SessionExpireWarningDialog.getResetJavascript());
	 *     return target;
	 * }
	 * </pre>
	 * 
	 * <p>
	 * This script will execute a no-op on pages without this behavior.
	 * </p>
	 * 
	 * @return
	 */
	public static String getResetJavascript() {
		return "if (typeof SessionExpireWarning != \"undefined\" && typeof SessionExpireWarning.reset == \"function\") { SessionExpireWarning.reset(); }";
	}
	
	public SessionExpireWarningDialog setWarningTime(int time) {
		this.warningTime = time;
		return this; // for chaining
	}
	
	public SessionExpireWarningDialog setResponseTime(int time) {
		this.responseTime = time;
		return this; // for chaining
	}
	
	


}