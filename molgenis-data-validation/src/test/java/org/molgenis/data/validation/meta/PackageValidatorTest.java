package org.molgenis.data.validation.meta;

import org.molgenis.data.meta.model.Package;
import org.molgenis.data.meta.system.SystemPackageRegistry;
import org.molgenis.data.validation.constraint.PackageConstraint;
import org.molgenis.data.validation.constraint.PackageConstraintViolation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.system.model.RootSystemPackage.PACKAGE_SYSTEM;
import static org.testng.Assert.assertEquals;

public class PackageValidatorTest
{
	private PackageValidator packageValidator;
	private SystemPackageRegistry systemPackageRegistry;
	private Package systemPackage;
	private Package testPackage;

	@BeforeMethod
	public void setUpBeforeMethod()
	{
		systemPackageRegistry = mock(SystemPackageRegistry.class);
		packageValidator = new PackageValidator(systemPackageRegistry);
		systemPackage = when(mock(Package.class).getId()).thenReturn(PACKAGE_SYSTEM).getMock();
		testPackage = when(mock(Package.class).getId()).thenReturn("test").getMock();
	}

	@Test
	public void testValidateNonSystemPackage() throws Exception
	{
		Package package_ = when(mock(Package.class).getId()).thenReturn("myPackage").getMock();
		when(systemPackageRegistry.containsPackage(package_)).thenReturn(false);
		assertEquals(packageValidator.validate(package_), emptyList());
	}

	@Test
	public void testValidateSystemPackageInRegistry() throws Exception
	{
		Package package_ = when(mock(Package.class).getId()).thenReturn(PACKAGE_SYSTEM + '_' + "myPackage").getMock();
		when(package_.getParent()).thenReturn(systemPackage);
		when(package_.getRootPackage()).thenReturn(systemPackage);
		when(systemPackageRegistry.containsPackage(package_)).thenReturn(true);
		assertEquals(packageValidator.validate(package_), emptyList());
	}

	@Test
	public void testValidateSystemPackageNotInRegistry() throws Exception
	{
		Package package_ = when(mock(Package.class).getId()).thenReturn(PACKAGE_SYSTEM + '_' + "myPackage").getMock();
		when(package_.getParent()).thenReturn(systemPackage);
		when(package_.getRootPackage()).thenReturn(systemPackage);
		when(systemPackageRegistry.containsPackage(package_)).thenReturn(false);
		assertEquals(packageValidator.validate(package_),
				singletonList(new PackageConstraintViolation(PackageConstraint.SYSTEM_PACKAGE_READ_ONLY, package_)));
	}

	@Test
	public void testValidatePackageInvalidName() throws Exception
	{
		Package package_ = when(mock(Package.class).getId()).thenReturn("0package").getMock();
		when(package_.getParent()).thenReturn(testPackage);
		when(package_.getRootPackage()).thenReturn(testPackage);
		assertEquals(packageValidator.validate(package_),
				singletonList(new PackageConstraintViolation(PackageConstraint.NAME, package_)));
	}

	@Test
	public void testValidatePackageValidName() throws Exception
	{
		Package package_ = when(mock(Package.class).getId()).thenReturn("test_myPackage").getMock();
		when(package_.getParent()).thenReturn(testPackage);
		when(package_.getRootPackage()).thenReturn(testPackage);
		assertEquals(packageValidator.validate(package_), emptyList());
	}

	@Test
	public void testValidatePackageValidNameNoParent() throws Exception
	{
		Package package_ = when(mock(Package.class).getId()).thenReturn("myPackage").getMock();
		when(package_.getParent()).thenReturn(null);
		assertEquals(packageValidator.validate(package_), emptyList());
	}
}