package gaia.rules.drools;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

public class QueryRelationship {
	protected Query lhs;
	protected Query rhs;
	protected Object relationship;
	protected BooleanClause.Occur occurrence;
	protected RelationshipType type;

	public QueryRelationship(Query lhs, Query rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Query getLhs() {
		return lhs;
	}

	public void setLhs(Query lhs) {
		this.lhs = lhs;
	}

	public Query getRhs() {
		return rhs;
	}

	public void setRhs(Query rhs) {
		this.rhs = rhs;
	}

	public Object getRelationship() {
		return relationship;
	}

	public void setRelationship(Object relationship) {
		this.relationship = relationship;
	}

	public BooleanClause.Occur getOccurrence() {
		return occurrence;
	}

	public void setOccurrence(BooleanClause.Occur occurrence) {
		this.occurrence = occurrence;
	}

	public RelationshipType getType() {
		return type;
	}

	public void setType(RelationshipType type) {
		this.type = type;
	}
}
