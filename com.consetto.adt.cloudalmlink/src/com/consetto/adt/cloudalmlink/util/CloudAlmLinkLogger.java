package com.consetto.adt.cloudalmlink.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * Centralized logging utility for the Cloud ALM Link plugin.
 * Uses Eclipse Platform logging infrastructure.
 */
public final class CloudAlmLinkLogger {

	private static final String PLUGIN_ID = "com.consetto.adt.cloudalmlink";

	private CloudAlmLinkLogger() {
		// Prevent instantiation
	}

	/**
	 * Gets the logger for this plugin.
	 *
	 * @return The Eclipse log, or null if bundle is not available
	 */
	private static ILog getLog() {
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		if (bundle != null) {
			return Platform.getLog(bundle);
		}
		return null;
	}

	/**
	 * Logs an error message with exception.
	 *
	 * @param message The error message
	 * @param e The exception that caused the error
	 */
	public static void logError(String message, Throwable e) {
		ILog log = getLog();
		if (log != null) {
			IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message, e);
			log.log(status);
		}
	}

	/**
	 * Logs an error message without exception.
	 *
	 * @param message The error message
	 */
	public static void logError(String message) {
		ILog log = getLog();
		if (log != null) {
			IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message);
			log.log(status);
		}
	}

	/**
	 * Logs a warning message.
	 *
	 * @param message The warning message
	 */
	public static void logWarning(String message) {
		ILog log = getLog();
		if (log != null) {
			IStatus status = new Status(IStatus.WARNING, PLUGIN_ID, message);
			log.log(status);
		}
	}

	/**
	 * Logs a warning message with exception.
	 *
	 * @param message The warning message
	 * @param e The exception
	 */
	public static void logWarning(String message, Throwable e) {
		ILog log = getLog();
		if (log != null) {
			IStatus status = new Status(IStatus.WARNING, PLUGIN_ID, message, e);
			log.log(status);
		}
	}

	/**
	 * Logs an informational message.
	 *
	 * @param message The info message
	 */
	public static void logInfo(String message) {
		ILog log = getLog();
		if (log != null) {
			IStatus status = new Status(IStatus.INFO, PLUGIN_ID, message);
			log.log(status);
		}
	}
}
