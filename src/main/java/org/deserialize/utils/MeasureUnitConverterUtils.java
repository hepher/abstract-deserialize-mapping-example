package org.deserialize.utils;

import java.util.Arrays;
import java.util.function.Function;

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
        // Base Unit
        G("g", "Grammi", Category.MASS, System.IS, 1.0, system -> {
            if (System.BIS.equals(system) || System.USC.equals(system)) {
                return 0.035274;
            }
            return 1.0;
        }, true),
        OZ("oz", "ounce", Category.MASS, System.BIS, 1.0, system -> {
            if (System.IS.equals(system)) {
                return 28.35;
            }
            return 1.0;
        }, true),

        // SI Unit
        KG("kg", "kilogrammi", Category.MASS, System.IS, 1000.0, null, false),

        // BIS System
        LB("lb", "pound", Category.MASS, System.BIS, 16.0, null, false);

        String constant;
        String description;
        Category category;
        System system;
        Double valueFromBase;
        Function<System, Double> valueForSystem;
        boolean baseUnit;

        UnitType(String constant, String description, Category category, System system, Double valueFromBase, Function<System, Double> valueForSystem, boolean baseUnit) {

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

        public String getDescription() {
            return description;
        }

        public Category getCategory() {
            return category;
        }

        public System getSystem() {
            return system;
        }

        public Double getValueFromBase() {
            return valueFromBase;
        }

        public boolean isBaseUnit() {
            return baseUnit;
        }

        public Double getValueForSystem(System system) {
            if (valueForSystem == null || system == null) {
                return null;
            }
            return valueForSystem.apply(system);
        }

        public static UnitType getBasicUnit(Category category, System system) {
            assert category != null;
            assert system != null;

            return Arrays.stream(values())
                    .filter(unitType -> unitType.category.equals(category) && unitType.system.equals(system))
                    .findFirst()
                    .orElseThrow();
        }

        public static UnitType get(String value) {
            if (value == null) {
                return null;
            }

            return valueOf(value.toUpperCase());
        }
    }

    public static Double convert(Double value, String originUnit, String destinationUnit) {
        assert value != null;
        assert originUnit != null;
        assert destinationUnit != null;

        UnitType originUnitType = UnitType.get(originUnit);
        UnitType destinationUnitType = UnitType.get(destinationUnit);

        // same Category ( example: cannot convert MASS to LENGTH )
        if (!originUnitType.getCategory().equals(destinationUnitType.getCategory())) {
            throw new RuntimeException("Invalid category match!");
        }

        // same System ( example: kg to g )
        if (originUnitType.getSystem().equals(destinationUnitType.getSystem())) {
            return value*(originUnitType.getValueFromBase()/destinationUnitType.getValueFromBase());
        }

        // different system ( example: kg to oz )
        // cast the origin value to base value of the same system
        UnitType originUnitBase = UnitType.getBasicUnit(originUnitType.getCategory(), originUnitType.getSystem());
        Double valueOriginBase = value*(originUnitType.getValueFromBase()/originUnitBase.getValueFromBase());

        // get and check that exist the origin
        Double originBaseValueForSystem = originUnitBase.getValueForSystem(destinationUnitType.getSystem());
        assert originBaseValueForSystem != null;

        // convert the base origin value to destination system base value
        double valueDestinationBase = valueOriginBase*originBaseValueForSystem;
        // find destination unit base
        UnitType destinationUnitBase = UnitType.getBasicUnit(destinationUnitType.getCategory(), destinationUnitType.getSystem());

        // convert to the final value
        return valueDestinationBase*(destinationUnitBase.getValueFromBase()/destinationUnitType.getValueFromBase());
    }

    public static void main(String[] args) {
        java.lang.System.out.println(convert(12.0, "kg", "g"));
        java.lang.System.out.println(convert(0.1, "kg", "g"));
        java.lang.System.out.println(convert(12.0, "g", "kg"));
        java.lang.System.out.println();

        java.lang.System.out.println(convert(12.0, "oz", "lb"));
        java.lang.System.out.println(convert(12.0, "lb", "oz"));

        java.lang.System.out.println();

        java.lang.System.out.println(convert(15.0, "kg", "lb"));
        java.lang.System.out.println(convert(15.0, "kg", "oz"));
    }
}
