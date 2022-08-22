package org.deserialize.utils;

import java.util.Arrays;
import java.util.function.Function;

/**
 * MeasureUnitConverterUtils allow converting a value with unit of measure in a measurement system to a value in another measurement system with equivalent unit of measure
 * <br>
 * The model is represented by 3 entity: <b>Category</b>, <b>System</b>, <b>Unit Type</b>
 * <br>
 * The System is the measurement system and has 3 values:
 * <br>
 * <ul>
 *     <li>International System</li>
 *     <li>British Imperial System</li>
 *     <li>United States Customary</li>
 * </ul>
 * <br>
 * The Category is a transversal concept between systems and is the type of measurement, and has 3 values:
 * <br>
 * <ul>
 *     <li>Volume</li>
 *     <li>Mass</li>
 *     <li>Length</li>
 * </ul>
 * <br>
 * The Unit Type is type of measurement in a single System, for example KG, G, OZ, LB. <br>
 * The most important concept for Unit Type is the <b>Base</b><br>, which was represented by a unit that has a unitary value in that System and Category, for example G or OZ<br>
 * The Unit Type has the follow properties: <br>
 * <ul>
 *     <li>constant: the constant that represent the measure unit </li>
 *     <li>description: description of constant </li>
 *     <li>category: category of the measure unit </li>
 *     <li>system: system of the measure unit </li>
 *     <li>valueFromBase: the unitary value as compared to base value in the same system and category ( the value for the base unit is 1.0 ) </li>
 *     <li>valueForSystem: the unitary value as compared to another base in another value ( only valid for base unit ) </li>
 *     <li>baseUnit: if the unit type is a base unit </li>
 * </ul>
 */
public class MeasureUnitConverterUtils {
    enum Category {
        VOLUME("Volume"), LENGTH("Length"), MASS("Mass");

        final String value;
        Category(String value) {
            this.value = value;
        }
        String getValue() {
            return value;
        }
    }

    enum System {
        IS("International System"), BIS("British Imperial System"), USC("United States Customary");

        final String value;
        System(String value) {
            this.value = value;
        }
        String getValue() {
            return value;
        }
    }

    enum UnitType {
        // IS system
        // LENGTH
        MM("mm", "millimeter", Category.LENGTH, System.IS, 0.001, null, false),
        CM("cm", "centimeter", Category.LENGTH, System.IS, 0.01, null, false),
        M("m", "meter", Category.LENGTH, System.IS, 1.0, system -> {
            if (System.BIS.equals(system) || System.USC.equals(system)) {
                return 1.09361; // yard
            }
            return 1.0;
        }, true),
        KM("km", "kilometer", Category.LENGTH, System.IS, 1000, null, false),
        // MASS
        G("g", "grams", Category.MASS, System.IS, 1.0, system -> {
            if (System.BIS.equals(system) || System.USC.equals(system)) {
                return 0.035274; // ounce
            }
            return 1.0;
        }, true),
        KG("kg", "kilograms", Category.MASS, System.IS, 1000.0, null, false),
        T("t", "Tonne", Category.MASS, System.IS, 1000000.0, null, false),
        // VOLUME
        ML("ml", "milliliter", Category.VOLUME, System.IS, 0.001, null, false),
        L("l", "liter", Category.VOLUME, System.IS, 1.0, system -> {
            if (System.BIS.equals(system) || System.USC.equals(system)) {
                return 0.879877; // quart
            }
            return 1.0;
        }, true),

        // BIS System
        // LENGTH
        IN("in", "inch", Category.LENGTH, System.BIS, 0.027778, null, false),
        FT("ft", "foot", Category.LENGTH, System.BIS, 0.333333, null, false),
        YD("yd", "yard", Category.LENGTH, System.BIS, 1.0, system -> {
            if (System.IS.equals(system)) {
                return 0.9144; //meter
            }
            return 1.0;
        }, true),
        MI("mi", "mile", Category.LENGTH, System.BIS, 1760, null, false),
        // MASS
        OZ("oz", "ounce", Category.MASS, System.BIS, 1.0, system -> {
            if (System.IS.equals(system)) {
                return 28.35; //grams
            }
            return 1.0;
        }, true),
        LB("lb", "pound", Category.MASS, System.BIS, 16.0, null, false),
        ST("st", "stone", Category.MASS, System.BIS, 224.0, null, false),
        // VOLUME
        FL_OZ("fl oz", "fluid ounce", Category.VOLUME, System.BIS, 0.025, null, false),
        PT("pt", "pint", Category.VOLUME, System.BIS, 0.5, null, false),
        QT("qt", "quart", Category.VOLUME, System.BIS, 1.0, system -> {
            if (System.IS.equals(system)) {
                return 1.13652; //liter
            }
            return 1.0;
        }, true),
        GAL("gal", "gallone", Category.VOLUME, System.BIS, 4.0, null, false),
        BBL("bbl", "barrel", Category.VOLUME, System.BIS, 168.0, null, false);

        String constant;
        String description;
        Category category;
        System system;
        double valueFromBase;
        Function<System, Double> valueForSystem;
        boolean baseUnit;

        UnitType(String constant, String description, Category category, System system, double valueFromBase, Function<System, Double> valueForSystem, boolean baseUnit) {

            assert constant != null;
            assert description != null;
            assert category != null;
            assert system != null;
            assert valueForSystem != null;

            this.constant = constant;
            this.description = description;
            this.category = category;
            this.system = system;
            this.valueFromBase = valueFromBase;
            this.valueForSystem = valueForSystem;
            this.baseUnit = baseUnit;
        }

        public String getConstant() {
            return constant;
        }

        public String getDescription() {
            return description;
        }

        public Category getCategory() {
            return category;
        }

        public System getSystem() {
            return system;
        }

        public double getValueFromBase() {
            return valueFromBase;
        }

        public boolean isBaseUnit() {
            return baseUnit;
        }

        public double getValueForSystem(System system) {
            assert valueForSystem != null;
            assert system != null;
            assert baseUnit;

            return valueForSystem.apply(system);
        }

        public static UnitType getBasicUnit(Category category, System system) {
            assert category != null;
            assert system != null;

            return Arrays.stream(values())
                    .filter(unitType -> unitType.category.equals(category) && unitType.system.equals(system) && unitType.isBaseUnit())
                    .findFirst()
                    .orElseThrow();
        }

        public static UnitType get(String value, System system) {
            assert value != null;
            assert system != null;

            return Arrays.stream(values())
                    .filter(unitType -> unitType.getSystem().equals(system) && unitType.getConstant().equals(value))
                    .findFirst()
                    .orElseThrow();
        }
    }

    /**
     * 2 cases: <br>
     * <ul>
     *     <li>1: origin unit and destination unit have same system</li>
     *     <li>2: origin unit and destination unit have different system</li>
     * </ul><br>
     * <b>Same System</b>: <br>
     * the result is: <b>value*originUnit.getValueFromBase/destinationUnit.getValueFromBase</b>
     * <br>
     * <b>Different System</b>: <br>
     * the conversion between 2 different system is divided in 3 step: <br>
     * <ul>
     *     <li>1. Conversion the value from origin unit type to origin base unit type ( for example from kg to g ) </li>
     *     <li>2. Conversion the result value from step 1 to destination base unit type </li>
     *     <li>3. Conversion the result value from step 2 to destination unit type</li>
     * </ul>
     * <br><br>
     *
     * @param value the value that want converting

     * @return the value converted to destination unit type
     */
    public static double convert(double value, UnitType originUnitType, UnitType destinationUnitType) {
        assert originUnitType != null;
        assert destinationUnitType != null;

        // different Category ( example: cannot convert MASS to LENGTH )
        if (!originUnitType.getCategory().equals(destinationUnitType.getCategory())) {
            throw new RuntimeException("Invalid category match!");
        }

        // same System ( example: kg to g )
        if (originUnitType.getSystem().equals(destinationUnitType.getSystem())) {
            return value*(originUnitType.getValueFromBase()/destinationUnitType.getValueFromBase());
        }

        // STEP 1
        // cast the origin value to base value of the same system
        UnitType originBaseUnitType = UnitType.getBasicUnit(originUnitType.getCategory(), originUnitType.getSystem());
        Double valueForBaseUnitType = value*originUnitType.getValueFromBase();

        // STEP 2
        // get the value for the origin
        Double originBaseValueForSystem = originBaseUnitType.getValueForSystem(destinationUnitType.getSystem());

        // convert the base origin value to destination system base value
        double baseValueForDestination = valueForBaseUnitType*originBaseValueForSystem;

        // STEP 3
        // convert to the final value
        return baseValueForDestination/destinationUnitType.getValueFromBase();
    }

    /**
     * overload of method convert
     * @param originUnit the unit type of the value
     * @param originSystem the value of the origin system {IS, BIS, USC}
     * @param destinationUnit the unit type to convert the value
     * @param destinationSystem the value of the destination system {IS, BIS, USC}
     * @return the value converted to destination unit type
     */
    public static double convert(double value, String originUnit, String originSystem, String destinationUnit, String destinationSystem) {
        assert originUnit != null;
        assert originSystem != null;
        assert destinationUnit != null;
        assert destinationSystem != null;

        return convert(value, UnitType.get(originUnit, System.valueOf(originSystem)), UnitType.get(destinationUnit, System.valueOf(destinationSystem)));
    }

    public static void main(String[] args) {
        java.lang.System.out.println(convert(12.0, "kg", "IS", "g", "IS"));
        java.lang.System.out.println(convert(0.1, "kg", "IS", "g", "IS"));
        java.lang.System.out.println(convert(12.0, "g", "IS", "kg", "IS"));
        java.lang.System.out.println();

        java.lang.System.out.println(convert(12.0, "oz", "BIS", "lb", "BIS"));
        java.lang.System.out.println(convert(12.0, "lb", "BIS", "oz", "BIS"));

        java.lang.System.out.println();

        java.lang.System.out.println(convert(15.0, "kg", "IS", "lb", "BIS"));
        java.lang.System.out.println(convert(15.0, "kg", "IS", "oz", "BIS"));
        java.lang.System.out.println(convert(15.0, "lb", "BIS", "kg", "IS"));
        java.lang.System.out.println(convert(15.0, "oz", "BIS", "kg", "IS"));

        java.lang.System.out.println();

        java.lang.System.out.println(convert(15.0, "l", "IS", "ml", "IS"));
        java.lang.System.out.println(convert(15.0, "ml", "IS", "l", "IS"));

        java.lang.System.out.println();

        java.lang.System.out.println(convert(11.0, "fl oz", "BIS", "qt", "BIS"));
        java.lang.System.out.println(convert(11.0, "qt", "BIS", "fl oz", "BIS"));

        java.lang.System.out.println();

        java.lang.System.out.println(convert(22.0, "l", "IS", "fl oz", "BIS"));
        java.lang.System.out.println(convert(22.0, "fl oz", "BIS", "qt", "BIS"));
        java.lang.System.out.println(convert(22.0, "fl oz", "BIS", "l", "IS"));
    }
}
