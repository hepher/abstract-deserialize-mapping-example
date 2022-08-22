window.Category =  {
    VOLUME: {
        constant: "VOLUME",
        description: "Volume"
    },
    LENGTH: {
        constant: "LENGTH",
        description: "Length"
    },
    MASS: {
        constant: "MASS",
        description: "Mass"
    },
    values: function() {
        let result = [];
        for (let prop in this) {
            if (typeof this[prop] !== "function") {
                result.push(this[prop]);
            }
        }

        return result;
    },
    valueOf: function (category) {
        if (category != null) {
            for (let prop in this) {
                if (typeof this[prop] !== "function") {
                    if (this[prop].constant === category) {
                        return this[prop];
                    }
                }
            }
        }

        return null;
    }
};

window.System = {
    IS: {
        constant: "IS",
        description: "International System"
    },
    BIS: {
        constant: "BIS",
        description: "British Imperial System"
    },
    USC: {
        constant: "USC",
        description: "United States Customary"
    },
    values: function () {
        let result = [];
        for (let prop in this) {
            if (typeof this[prop] !== "function") {
                result.push(this[prop]);
            }
        }

        return result;
    },
    valueOf: function (system) {
        if (system != null) {
            for (let prop in this) {
                if (typeof this[prop] !== "function") {
                    if (this[prop].constant === system) {
                        return this[prop];
                    }
                }
            }
        }

        return null;
    }
}

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
window.MeasureUnitConverterUtils = {
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
     * @param originUnit the unit type of the value
     * @param originSystem the value of the origin system {IS, BIS, USC}
     * @param destinationUnit the unit type to convert the value
     * @param destinationSystem the value of the destination system {IS, BIS, USC}
     * @return the value converted to destination unit type
     */
    convert: function(value, originUnit, originSystem, destinationUnit, destinationSystem) {
        if (value == null || originUnit == null || originSystem == null || destinationUnit == null || destinationSystem == null) {
            throw 'Input parameters cannot be blank';
        }

        let originUnitType = this.UnitType.valueOf(originUnit, System.valueOf(originSystem));
        let destinationUnitType = this.UnitType.valueOf(destinationUnit, System.valueOf(destinationSystem));

        // different Category ( example: cannot convert MASS to LENGTH )
        if (originUnitType.category.constant !== destinationUnitType.category.constant) {
            throw 'Invalid category match!';
        }

        // same System ( example: kg to g )
        if (originUnitType.system.constant === destinationUnitType.system.constant) {
            return value*(originUnitType.valueFromBase/destinationUnitType.valueFromBase);
        }

        // STEP 1
        // cast the origin value to base value of the same system
        let originBaseUnitType = this.UnitType.getBasicUnit(originUnitType.category, originUnitType.system);
        let valueForBaseUnitType = value*(originUnitType.valueFromBase/originBaseUnitType.valueFromBase);

        // STEP 2
        // get and check that exist the origin
        let originBaseValueForSystem = this.UnitType.getValueForSystem(originBaseUnitType, destinationUnitType.system);
        if (originBaseValueForSystem == null) {
            throw 'The base value cannot be found!';
        }

        // convert the base origin value to destination system base value
        let baseValueForDestination = valueForBaseUnitType*originBaseValueForSystem;

        // STEP 3
        // find destination unit base
        let destinationBaseUnit = this.UnitType.getBasicUnit(destinationUnitType.category, destinationUnitType.system);

        // convert to the final value
        return baseValueForDestination*(destinationBaseUnit.valueFromBase/destinationUnitType.valueFromBase);
    },
    UnitType: {
        getBasicUnit: function (category, system) {
            for (let prop in this) {
                if (typeof this[prop] !== "function") {
                    if (this[prop].baseUnit && this[prop].category.constant === category.constant && this[prop].system.constant === system.constant) {
                        return this[prop];
                    }
                }
            }

            throw 'basic unit not found';
        },
        values: function () {
            let result = [];
            for (let prop in this) {
                if (typeof this[prop] !== "function") {
                    result.push(this[prop]);
                }
            }

            return result;
        },
        valueOf: function (unitConstant, system) {
            if (unitConstant != null) {
                for (let prop in this) {
                    if (typeof this[prop] !== "function") {
                        if (this[prop].constant === unitConstant && this[prop].system.constant === system.constant) {
                            return this[prop];
                        }
                    }
                }
            }

            return null;
        },
        getValueForSystem: function(unitType, system) {
          if (system == null) {
              return null;
          }

          return unitType.valueForSystem(system);
        },
        newConstantValue: function (constant, description, category, system, valueFromBase, valueForSystem, baseUnit) {
            return {
                constant: constant,
                description: description,
                category: category,
                system: system,
                valueFromBase: valueFromBase,
                valueForSystem: valueForSystem,
                baseUnit: baseUnit
            }
        },
        // IS System
        G: {
            constant: "g",
            description: "grams",
            category: Category.MASS,
            system: System.IS,
            valueFromBase: 1.0,
            valueForSystem: function (system) {
                switch (system.constant) {
                    case System.BIS.constant:
                    case System.USC.constant:
                        return 0.035274;
                    default:
                        return 1;
                }
            },
            baseUnit: true
        },
        OZ: {
            constant: "oz",
            description: "ounce",
            category: Category.MASS,
            system: System.BIS,
            valueFromBase: 1.0,
            valueForSystem: function (system) {
                switch (system.constant) {
                    case System.IS.constant:
                        return 28.35;
                    default:
                        return 1;
                }
            },
            baseUnit: true
        },
        // IS System
        KG: {
            constant: "kg",
            description: "kilo grammi",
            category: Category.MASS,
            system: System.IS,
            valueFromBase: 1000.0,
            valueForSystem: null,
            baseUnit: false
        },
        LB: {
            constant: "lb",
            description: "pound grammi",
            category: Category.MASS,
            system: System.BIS,
            valueFromBase: 16.0,
            valueForSystem: null,
            baseUnit: false
        }
    }
}