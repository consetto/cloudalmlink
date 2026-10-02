package com.consetto.adt.cloudalmlink.core;

/**
 * An atom link of an ADT object: relation and target.
 *
 * @param rel  The link relation, e.g. "http://www.sap.com/adt/relations/versions"
 * @param href The link target, absolute or relative to the object
 */
public record AtomLink(String rel, String href) {
}
