package org.molgenis.data.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.springframework.data.domain.Sort;

public class QueryImpl implements Query
{
	protected final Vector<List<QueryRule>> rules = new Vector<List<QueryRule>>();
	final ArrayList<QueryRule> order = new ArrayList<QueryRule>();

	private Iterator<String> attributeNames;
	private int pageSize;
	private int offset;
	private Sort sort;

	public QueryImpl()
	{
		this.rules.add(new ArrayList<QueryRule>());
	}

	public void addRule(QueryRule addRules)
	{
		this.rules.lastElement().add(addRules);
	}

	@Override
	public List<QueryRule> getRules()
	{
		if (this.rules.size() > 1) throw new MolgenisDataException(
				"Nested query not closed, use unnest() or unnestAll()");
		if (this.rules.size() > 0)
		{
			List<QueryRule> rules = this.rules.lastElement();

			return rules;
		}
		else return new ArrayList<QueryRule>();
	}

	public void setAttributeNames(Iterator<String> attributeNames)
	{
		this.attributeNames = attributeNames;
	}

	@Override
	public Iterator<String> getAttributeNames()
	{
		return attributeNames;
	}

	public void setPageSize(int pageSize)
	{
		this.pageSize = pageSize;
	}

	@Override
	public int getPageSize()
	{
		return pageSize;
	}

	@Override
	public int getOffset()
	{
		return offset;
	}

	public void setOffset(int offset)
	{
		this.offset = offset;
	}

	public void setSort(Sort sort)
	{
		this.sort = sort;
	}

	@Override
	public Sort getSort()
	{
		return sort;
	}

	@Override
	public Query search(String searchTerms)
	{
		rules.lastElement().add(new QueryRule(Operator.SEARCH, searchTerms));
		return this;
	}

	@Override
	public Query or()
	{
		rules.lastElement().add(new QueryRule(Operator.OR));
		return this;
	}

	@Override
	public Query and()
	{
		return this;
	}

	@Override
	public Query like(String field, Object value)
	{
		rules.lastElement().add(new QueryRule(field, Operator.LIKE, value));
		return this;
	}

	@Override
	public Query eq(String field, Object value)
	{
		rules.lastElement().add(new QueryRule(field, Operator.EQUALS, value));
		return this;
	}

	@Override
	public Query in(String field, Iterable<?> objectIterator)
	{
		rules.lastElement().add(new QueryRule(field, Operator.IN, objectIterator));
		return this;
	}

	@Override
	public Query gt(String field, Object value)
	{
		rules.lastElement().add(new QueryRule(field, Operator.GREATER, value));
		return this;
	}

	@Override
	public Query ge(String field, Object value)
	{
		rules.lastElement().add(new QueryRule(field, Operator.GREATER_EQUAL, value));
		return this;
	}

	@Override
	public Query le(String field, Object value)
	{
		rules.lastElement().add(new QueryRule(field, Operator.LESS_EQUAL, value));
		return this;
	}

	@Override
	public Query lt(String field, Object value)
	{
		rules.lastElement().add(new QueryRule(field, Operator.LESS, value));
		return this;
	}

	@Override
	public Query nest()
	{
		// add element to our nesting list...
		this.rules.add(new ArrayList<QueryRule>());
		return this;
	}

	@Override
	public Query unnest()
	{
		if (this.rules.size() == 1) throw new MolgenisDataException("Cannot unnest root element of query");

		// remove last element and add to parent as nested rule
		QueryRule nested = new QueryRule(Operator.NESTED, this.rules.lastElement());
		this.rules.remove(this.rules.lastElement());
		this.rules.lastElement().add(nested);
		return this;
	}

	@Override
	public Query unnestAll()
	{
		while (this.rules.size() > 1)
		{
			unnest();
		}
		return this;
	}

	@Override
	public Query rng(String field, Object smaller, Object bigger)
	{
		this.gt(field, smaller);
		this.lt(field, bigger);
		return this;
	}

	@Override
	public String toString()
	{
		return "QueryImpl [rules=" + rules + ", order=" + order + ", attributeNames=" + attributeNames + ", pageSize="
				+ pageSize + ", offset=" + offset + ", sort=" + sort + "]";
	}

}
