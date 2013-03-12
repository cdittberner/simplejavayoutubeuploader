/**
 * This class is generated by jOOQ
 */
package org.chaosfisch.youtubeuploader.db.generated.tables.records;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = {"http://www.jooq.org", "3.0.0"},
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked" })
public class PlaylistRecord extends org.jooq.impl.UpdatableRecordImpl<org.chaosfisch.youtubeuploader.db.generated.tables.records.PlaylistRecord> implements org.jooq.Record10<java.lang.Integer, java.lang.String, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.lang.Integer, java.sql.Timestamp> {

	private static final long serialVersionUID = -452360584;

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.ID</code>. 
	 */
	public void setId(java.lang.Integer value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.ID</code>. 
	 */
	public java.lang.Integer getId() {
		return (java.lang.Integer) getValue(0);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.PKEY</code>. 
	 */
	public void setPkey(java.lang.String value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.PKEY</code>. 
	 */
	public java.lang.String getPkey() {
		return (java.lang.String) getValue(1);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.PRIVATE</code>. 
	 */
	public void setPrivate(java.lang.Boolean value) {
		setValue(2, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.PRIVATE</code>. 
	 */
	public java.lang.Boolean getPrivate() {
		return (java.lang.Boolean) getValue(2);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.TITLE</code>. 
	 */
	public void setTitle(java.lang.String value) {
		setValue(3, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.TITLE</code>. 
	 */
	public java.lang.String getTitle() {
		return (java.lang.String) getValue(3);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.URL</code>. 
	 */
	public void setUrl(java.lang.String value) {
		setValue(4, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.URL</code>. 
	 */
	public java.lang.String getUrl() {
		return (java.lang.String) getValue(4);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.THUMBNAIL</code>. 
	 */
	public void setThumbnail(java.lang.String value) {
		setValue(5, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.THUMBNAIL</code>. 
	 */
	public java.lang.String getThumbnail() {
		return (java.lang.String) getValue(5);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.NUMBER</code>. 
	 */
	public void setNumber(java.lang.Integer value) {
		setValue(6, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.NUMBER</code>. 
	 */
	public java.lang.Integer getNumber() {
		return (java.lang.Integer) getValue(6);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.SUMMARY</code>. 
	 */
	public void setSummary(java.lang.String value) {
		setValue(7, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.SUMMARY</code>. 
	 */
	public java.lang.String getSummary() {
		return (java.lang.String) getValue(7);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.ACCOUNT_ID</code>. 
	 */
	public void setAccountId(java.lang.Integer value) {
		setValue(8, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.ACCOUNT_ID</code>. 
	 */
	public java.lang.Integer getAccountId() {
		return (java.lang.Integer) getValue(8);
	}

	/**
	 * Setter for <code>PUBLIC.PLAYLIST.MODIFIED</code>. 
	 */
	public void setModified(java.sql.Timestamp value) {
		setValue(9, value);
	}

	/**
	 * Getter for <code>PUBLIC.PLAYLIST.MODIFIED</code>. 
	 */
	public java.sql.Timestamp getModified() {
		return (java.sql.Timestamp) getValue(9);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Record1<java.lang.Integer> key() {
		return (org.jooq.Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Record10 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row10<java.lang.Integer, java.lang.String, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.lang.Integer, java.sql.Timestamp> fieldsRow() {
		return (org.jooq.Row10) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row10<java.lang.Integer, java.lang.String, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.lang.Integer, java.sql.Timestamp> valuesRow() {
		return (org.jooq.Row10) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field1() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field2() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.PKEY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Boolean> field3() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.PRIVATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field4() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.TITLE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field5() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.URL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field6() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.THUMBNAIL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field7() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.NUMBER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field8() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.SUMMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field9() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.ACCOUNT_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.sql.Timestamp> field10() {
		return org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST.MODIFIED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value1() {
		return getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value2() {
		return getPkey();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Boolean value3() {
		return getPrivate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value4() {
		return getTitle();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value5() {
		return getUrl();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value6() {
		return getThumbnail();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value7() {
		return getNumber();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value8() {
		return getSummary();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value9() {
		return getAccountId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.sql.Timestamp value10() {
		return getModified();
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached PlaylistRecord
	 */
	public PlaylistRecord() {
		super(org.chaosfisch.youtubeuploader.db.generated.tables.Playlist.PLAYLIST);
	}
}