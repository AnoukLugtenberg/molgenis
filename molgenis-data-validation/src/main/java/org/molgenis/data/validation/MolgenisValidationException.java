package org.molgenis.data.validation;

import com.google.common.collect.Collections2;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.MolgenisDataAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Deprecated // FIXME extend from LocalizedRuntimeException
public class MolgenisValidationException extends MolgenisDataAccessException
{
	private static final long serialVersionUID = 1L;
	private final Set<ConstraintViolation> violations;

	public MolgenisValidationException(ConstraintViolation violation)
	{
		this(Collections.singleton(violation));
	}

	public MolgenisValidationException(Set<ConstraintViolation> violations)
	{
		this.violations = violations;
	}

	public Set<ConstraintViolation> getViolations()
	{
		return violations;
	}

	@Override
	public String getMessage()
	{
		if ((violations == null) || (violations.isEmpty())) return "Unknown validation exception.";

		return StringUtils.join(Collections2.transform(violations, ConstraintViolation::getMessage), '.');
	}

	/**
	 * renumber the violation row indices with the actual row numbers
	 */
	public void renumberViolationRowIndices(List<Integer> actualIndices)
	{
		violations.forEach(v -> v.renumberRowIndex(actualIndices));
	}
}
