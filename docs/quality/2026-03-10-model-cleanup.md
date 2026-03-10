# Model Cleanup Report

Generated during issue #413: Clean up demo models

## Summary

- Models processed: 34
- Names transformed (underscore → space): 817
- Metadata fields fixed: 25 TU Delft models updated with author, source, license, URL
- Licenses corrected: 10 models changed from "unknown" to "CC-BY-NC-SA-4.0"
- Warnings found: 116 (missing units, disconnected flows, missing metadata on non-Delft models)
- Equations left unchanged (engine resolves underscore↔space at runtime)

## Per-Model Details

### demographics\aging-chain.json [TU Delft]

**Changes:**
  Flow: 'adults_net_flow' → 'adults net flow'
  Flow: 'children_net_flow' → 'children net flow'
  Flow: 'retirees_net_flow' → 'retirees net flow'
  Aux: 'grey_pressure' → 'grey pressure'
  Aux: 'total_population' → 'total population'
  Aux: 'inactive_population' → 'inactive population'
  Aux: 'average_retiree_period' → 'average retiree period'
  Aux: 'burden_per_active_adult' → 'burden per active adult'
  Aux: 'adults_not_on_labour_market' → 'adults not on labour market'
  Aux: 'adults_on_the_labour_market' → 'adults on the labour market'
  Aux: 'average_birth_rate_per_adult' → 'average birth rate per adult'
  Aux: 'average_childhood_period' → 'average childhood period'
  Aux: 'average_adult_participation_ratio' → 'average adult participation ratio'
  Aux: 'average_adult_period' → 'average adult period'
  Aux: 'initial_number_of_retirees' → 'initial number of retirees'
  Aux: 'initial_number_of_children' → 'initial number of children'
  Aux: 'initial_number_of_adults' → 'initial number of adults'
  Lookup: 'average_retiree_period_lookup' → 'average retiree period lookup'
  Metadata fixed: author, source, url

**Warnings:**
  WARNING: Auxiliary 'total population' has no unit
  WARNING: Auxiliary 'adults not on labour market' has no unit

### demographics\family-planning.json [TU Delft]

**Changes:**
  Flow: 'adults_net_flow' → 'adults net flow'
  Flow: 'kids_net_flow' → 'kids net flow'
  Flow: 'unborns_net_flow' → 'unborns net flow'
  Flow: 'retirees_net_flow' → 'retirees net flow'
  Flow: 'youngsters_net_flow' → 'youngsters net flow'
  Aux: 'criminal_kids' → 'criminal kids'
  Aux: 'criminal_youngsters' → 'criminal youngsters'
  Aux: 'criminal_adults' → 'criminal adults'
  Aux: 'criminal_retirees' → 'criminal retirees'
  Aux: 'total_multiproblem_family_population' → 'total multiproblem family population'
  Aux: 'early_ended_pregnancies' → 'early ended pregnancies'
  Aux: 'from_adults_to_retirees' → 'from adults to retirees'
  Aux: 'from_young_adults_to_adults' → 'from young adults to adults'
  Aux: 'growing_up' → 'growing up'
  Aux: 'percentage_of_youngsters_with_criminal_behavior' → 'percentage of youngsters with criminal behavior'
  Aux: 'crimes_by_others' → 'crimes by others'
  Aux: 'ciminal_acts_per_criminal_kid' → 'ciminal acts per criminal kid'
  Aux: 'ciminal_acts_per_criminal_youngster' → 'ciminal acts per criminal youngster'
  Aux: 'annual_fertility_rate_adults' → 'annual fertility rate adults'
  Aux: 'annual_fertility_rate_youngsters' → 'annual fertility rate youngsters'
  Aux: 'average_time_as_adult' → 'average time as adult'
  Aux: 'average_time_as_kid' → 'average time as kid'
  Aux: 'average_time_as_retirees' → 'average time as retirees'
  Aux: 'average_time_as_youngster' → 'average time as youngster'
  Aux: 'average_time_pregnancy' → 'average time pregnancy'
  Aux: 'percentage_early_ended_pregnancies' → 'percentage early ended pregnancies'
  Aux: 'criminal_acts_per_criminal_adult' → 'criminal acts per criminal adult'
  Aux: 'criminal_acts_per_criminal_retiree' → 'criminal acts per criminal retiree'
  Aux: 'ini_youngsters' → 'ini youngsters'
  Aux: 'ini_adults' → 'ini adults'
  Aux: 'ini_kids' → 'ini kids'
  Aux: 'percentage_of_adults_with_criminal_behavior' → 'percentage of adults with criminal behavior'
  Aux: 'percentage_of_retirees_with_criminal_behavior' → 'percentage of retirees with criminal behavior'
  Aux: 'percentage_of_kids_and_youngsters_with_criminal_behavior' → 'percentage of kids and youngsters with criminal behavior'
  Aux: 'ini_unborns' → 'ini unborns'
  Aux: 'ini_retirees' → 'ini retirees'
  Metadata fixed: author, source, url

**Warnings:**
  WARNING: Stock 'adults' has no unit
  WARNING: Stock 'kids' has no unit
  WARNING: Stock 'unborns' has no unit
  WARNING: Stock 'retirees' has no unit
  WARNING: Stock 'youngsters' has no unit
  WARNING: Auxiliary 'crimes' has no unit
  WARNING: Auxiliary 'criminal kids' has no unit
  WARNING: Auxiliary 'criminal youngsters' has no unit
  WARNING: Auxiliary 'births' has no unit
  WARNING: Auxiliary 'criminal adults' has no unit
  WARNING: Auxiliary 'criminal retirees' has no unit
  WARNING: Auxiliary 'total multiproblem family population' has no unit
  WARNING: Auxiliary 'deaths' has no unit
  WARNING: Auxiliary 'early ended pregnancies' has no unit
  WARNING: Auxiliary 'from adults to retirees' has no unit
  WARNING: Auxiliary 'from young adults to adults' has no unit
  WARNING: Auxiliary 'growing up' has no unit
  WARNING: Auxiliary 'conceptions' has no unit
  WARNING: Auxiliary 'percentage of youngsters with criminal behavior' has no unit
  WARNING: Auxiliary 'crimes by others' has no unit
  WARNING: Auxiliary 'ciminal acts per criminal kid' has no unit
  WARNING: Auxiliary 'ciminal acts per criminal youngster' has no unit
  WARNING: Auxiliary 'annual fertility rate adults' has no unit
  WARNING: Auxiliary 'annual fertility rate youngsters' has no unit
  WARNING: Auxiliary 'average time as adult' has no unit
  WARNING: Auxiliary 'average time as kid' has no unit
  WARNING: Auxiliary 'average time as retirees' has no unit
  WARNING: Auxiliary 'average time as youngster' has no unit
  WARNING: Auxiliary 'average time pregnancy' has no unit
  WARNING: Auxiliary 'percentage early ended pregnancies' has no unit
  WARNING: Auxiliary 'criminal acts per criminal adult' has no unit
  WARNING: Auxiliary 'criminal acts per criminal retiree' has no unit
  WARNING: Auxiliary 'ini youngsters' has no unit
  WARNING: Auxiliary 'ini adults' has no unit
  WARNING: Auxiliary 'ini kids' has no unit
  WARNING: Auxiliary 'percentage of adults with criminal behavior' has no unit
  WARNING: Auxiliary 'percentage of retirees with criminal behavior' has no unit
  WARNING: Auxiliary 'percentage of kids and youngsters with criminal behavior' has no unit
  WARNING: Auxiliary 'ini unborns' has no unit
  WARNING: Auxiliary 'ini retirees' has no unit

### demographics\higher-education-stimuli.json [TU Delft]

**Changes:**
  Stock: 'MSc_studenten' → 'MSc studenten'
  Stock: 'BSc_studenten' → 'BSc studenten'
  Flow: 'financien_net_flow' → 'financien net flow'
  Flow: 'MSc_studenten_net_flow' → 'MSc studenten net flow'
  Flow: 'docenten_net_flow' → 'docenten net flow'
  Flow: 'BSc_studenten_net_flow' → 'BSc studenten net flow'
  Aux: 'uitstroom_van_onderwijsgelden' → 'uitstroom van onderwijsgelden'
  Aux: 'instroom_BSc_studenten' → 'instroom BSc studenten'
  Aux: 'instroom_MSc_studenten' → 'instroom MSc studenten'
  Aux: 'fractie_afvallers_MSc' → 'fractie afvallers MSc'
  Aux: 'fractie_afvallers_BSc' → 'fractie afvallers BSc'
  Aux: 'boete_per_langstudeerder' → 'boete per langstudeerder'
  Aux: 'aantal_docenturen_per_student' → 'aantal docenturen per student'
  Aux: 'afvallers_MSc' → 'afvallers MSc'
  Aux: 'jaarlijkse_MSc_instroom' → 'jaarlijkse MSc instroom'
  Aux: 'bijkomende_jaarlijkse_vertraging_BSc' → 'bijkomende jaarlijkse vertraging BSc'
  Aux: 'bijkomende_jaarlijkse_vertraging_MSc' → 'bijkomende jaarlijkse vertraging MSc'
  Aux: 'BSc_uitstroom_na_vaste_en_bijkomende_vertraging' → 'BSc uitstroom na vaste en bijkomende vertraging'
  Aux: 'MSc_uitstroom_na_vaste_en_bijkomende_vertraging' → 'MSc uitstroom na vaste en bijkomende vertraging'
  Aux: 'netto_werving_van_docenten' → 'netto werving van docenten'
  Aux: 'instroom_onderwijsgelden' → 'instroom onderwijsgelden'
  Aux: 'evolutie_van_de_nieuwe_MSc_instromers' → 'evolutie van de nieuwe MSc instromers'
  Aux: 'instroommoment_MSc' → 'instroommoment MSc'
  Aux: 'fractie_langstudeerder' → 'fractie langstudeerder'
  Aux: 'fractie_van_BSc_naar_MSc' → 'fractie van BSc naar MSc'
  Aux: 'uitstroom_MSc_studenten' → 'uitstroom MSc studenten'
  Aux: 'MSc_uitstroom_indien_enkel_vaste_vertraging_door_minimale_studieduur' → 'MSc uitstroom indien enkel vaste vertraging door minimale studieduur'
  Aux: 'totaal_aantal_studenten' → 'totaal aantal studenten'
  Aux: 'maximaal_aantal_docenten' → 'maximaal aantal docenten'
  Aux: 'afvallers_BSc' → 'afvallers BSc'
  Aux: 'evolutie_van_de_nieuwe_BSc_instromers' → 'evolutie van de nieuwe BSc instromers'
  Aux: 'BSc_instroommoment' → 'BSc instroommoment'
  Aux: 'jaarlijkse_BSc_instroom' → 'jaarlijkse BSc instroom'
  Aux: 'uitstroom_BSc_studenten' → 'uitstroom BSc studenten'
  Aux: 'BSc_uitstroom_indien_enkel_vaste_vertraging_door_minimale_studieduur' → 'BSc uitstroom indien enkel vaste vertraging door minimale studieduur'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'hoogte_boete_per_langstudeerder' → 'hoogte boete per langstudeerder'
  Aux: 'jaarlijkse_lump_sum_en_andere_gelden' → 'jaarlijkse lump sum en andere gelden'
  Aux: 'subsidie_per_BSc_afstudeerder' → 'subsidie per BSc afstudeerder'
  Aux: 'subsidie_per_BSc_instromer' → 'subsidie per BSc instromer'
  Aux: 'subsidie_per_MSc_afstudeerder' → 'subsidie per MSc afstudeerder'
  Aux: 'subsidie_per_MSc_instromer' → 'subsidie per MSc instromer'
  Aux: 'gemiddelde_kost_per_docent' → 'gemiddelde kost per docent'
  Aux: 'minimale_MSc_studieduur' → 'minimale MSc studieduur'
  Aux: 'minimale_BSc_studieduur' → 'minimale BSc studieduur'
  Lookup: 'evolutie_van_de_nieuwe_MSc_instromers_lookup' → 'evolutie van de nieuwe MSc instromers lookup'
  Lookup: 'fractie_langstudeerder_lookup' → 'fractie langstudeerder lookup'
  Lookup: 'kwaliteit_lookup' → 'kwaliteit lookup'
  Lookup: 'evolutie_van_de_nieuwe_BSc_instromers_lookup' → 'evolutie van de nieuwe BSc instromers lookup'
  Metadata fixed: author, source, url

**Warnings:**
  WARNING: Stock 'docenten' has no unit
  WARNING: Auxiliary 'uitstroom van onderwijsgelden' has no unit
  WARNING: Auxiliary 'instroom BSc studenten' has no unit
  WARNING: Auxiliary 'instroom MSc studenten' has no unit
  WARNING: Auxiliary 'fractie afvallers MSc' has no unit
  WARNING: Auxiliary 'fractie afvallers BSc' has no unit
  WARNING: Auxiliary 'boete per langstudeerder' has no unit
  WARNING: Auxiliary 'aantal docenturen per student' has no unit
  WARNING: Auxiliary 'afvallers MSc' has no unit
  WARNING: Auxiliary 'jaarlijkse MSc instroom' has no unit
  WARNING: Auxiliary 'bijkomende jaarlijkse vertraging BSc' has no unit
  WARNING: Auxiliary 'bijkomende jaarlijkse vertraging MSc' has no unit
  WARNING: Auxiliary 'BSc uitstroom na vaste en bijkomende vertraging' has no unit
  WARNING: Auxiliary 'MSc uitstroom na vaste en bijkomende vertraging' has no unit
  WARNING: Auxiliary 'netto werving van docenten' has no unit
  WARNING: Auxiliary 'instroom onderwijsgelden' has no unit
  WARNING: Auxiliary 'evolutie van de nieuwe MSc instromers' has no unit
  WARNING: Auxiliary 'instroommoment MSc' has no unit
  WARNING: Auxiliary 'fractie langstudeerder' has no unit
  WARNING: Auxiliary 'fractie van BSc naar MSc' has no unit
  WARNING: Auxiliary 'MSc uitstroom indien enkel vaste vertraging door minimale studieduur' has no unit
  WARNING: Auxiliary 'totaal aantal studenten' has no unit
  WARNING: Auxiliary 'kwaliteit' has no unit
  WARNING: Auxiliary 'maximaal aantal docenten' has no unit
  WARNING: Auxiliary 'afvallers BSc' has no unit
  WARNING: Auxiliary 'evolutie van de nieuwe BSc instromers' has no unit
  WARNING: Auxiliary 'BSc instroommoment' has no unit
  WARNING: Auxiliary 'jaarlijkse BSc instroom' has no unit
  WARNING: Auxiliary 'BSc uitstroom indien enkel vaste vertraging door minimale studieduur' has no unit
  WARNING: Auxiliary 'hoogte boete per langstudeerder' has no unit
  WARNING: Auxiliary 'jaarlijkse lump sum en andere gelden' has no unit
  WARNING: Auxiliary 'subsidie per MSc instromer' has no unit

### demographics\new-towns.json [TU Delft]

**Changes:**
  Flow: 'bevolking_net_flow' → 'bevolking net flow'
  Flow: 'bedrijven_net_flow' → 'bedrijven net flow'
  Flow: 'huizen_net_flow' → 'huizen net flow'
  Aux: 'slummultiplicator_voor_migratie' → 'slummultiplicator voor migratie'
  Aux: 'afbraak_van_huizen' → 'afbraak van huizen'
  Aux: 'leegstandsgraad_van_huizen' → 'leegstandsgraad van huizen'
  Aux: 'effect_schaarste_landgebruik_op_intensiviteit_bedrijfsgebouwen' → 'effect schaarste landgebruik op intensiviteit bedrijfsgebouwen'
  Aux: 'extra_afbraakgraad' → 'extra afbraakgraad'
  Aux: 'verhouding_huishoudens_tot_huizen' → 'verhouding huishoudens tot huizen'
  Aux: 'constructie_van_huizen' → 'constructie van huizen'
  Aux: 'constructie_van_bedrijfsgebouwen' → 'constructie van bedrijfsgebouwen'
  Aux: 'jobbeschikbaarheids_multiplicator_voor_immigratie' → 'jobbeschikbaarheids multiplicator voor immigratie'
  Aux: 'huizenbeschikbaarheids_multiplicator_voor_immigratie' → 'huizenbeschikbaarheids multiplicator voor immigratie'
  Aux: 'afbraak_van_bedrijfsgebouwen' → 'afbraak van bedrijfsgebouwen'
  Aux: 'beroepsbevolkings_multiplicator_voor_bedrijven' → 'beroepsbevolkings multiplicator voor bedrijven'
  Aux: 'landbeschikbaarheids_multiplicator_voor_bedrijfsgebouwen' → 'landbeschikbaarheids multiplicator voor bedrijfsgebouwen'
  Aux: 'huizenschaarste_multiplicator' → 'huizenschaarste multiplicator'
  Aux: 'landbeschikbaarheids_multiplicator_voor_huizen' → 'landbeschikbaarheids multiplicator voor huizen'
  Aux: 'beroepsbevolking_tot_jobs_ratio' → 'beroepsbevolking tot jobs ratio'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'switch_jobs' → 'switch jobs'
  Aux: 'grondgebruik_per_bedrijf' → 'grondgebruik per bedrijf'
  Aux: 'totale_grondoppervlakte' → 'totale grondoppervlakte'
  Aux: 'switch_leegstandseffect' → 'switch leegstandseffect'
  Aux: 'constructiegraad_van_bedrijfsgebouwen' → 'constructiegraad van bedrijfsgebouwen'
  Aux: 'afbraakgraad_van_bedrijfsgebouwen' → 'afbraakgraad van bedrijfsgebouwen'
  Aux: 'constructiegraad_van_huizen' → 'constructiegraad van huizen'
  Aux: 'afbraakgraad_van_huizen' → 'afbraakgraad van huizen'
  Aux: 'gemiddelde_grootte_van_huishoudens' → 'gemiddelde grootte van huishoudens'
  Aux: 'normale_immigratiegraad' → 'normale immigratiegraad'
  Aux: 'initieel_aantal_jobs_per_bedrijfsgebouw' → 'initieel aantal jobs per bedrijfsgebouw'
  Aux: 'grondgebruik_per_huis' → 'grondgebruik per huis'
  Aux: 'normale_emigratiegraad' → 'normale emigratiegraad'
  Lookup: 'slummultiplicator_voor_migratie_lookup' → 'slummultiplicator voor migratie lookup'
  Lookup: 'effect_schaarste_landgebruik_op_intensiviteit_bedrijfsgebouwen_lookup' → 'effect schaarste landgebruik op intensiviteit bedrijfsgebouwen lookup'
  Lookup: 'extra_afbraakgraad_lookup' → 'extra afbraakgraad lookup'
  Lookup: 'jobbeschikbaarheids_multiplicator_voor_immigratie_lookup' → 'jobbeschikbaarheids multiplicator voor immigratie lookup'
  Lookup: 'huizenbeschikbaarheids_multiplicator_voor_immigratie_lookup' → 'huizenbeschikbaarheids multiplicator voor immigratie lookup'
  Lookup: 'beroepsbevolkings_multiplicator_voor_bedrijven_lookup' → 'beroepsbevolkings multiplicator voor bedrijven lookup'
  Lookup: 'landbeschikbaarheids_multiplicator_voor_bedrijfsgebouwen_lookup' → 'landbeschikbaarheids multiplicator voor bedrijfsgebouwen lookup'
  Lookup: 'huizenschaarste_multiplicator_lookup' → 'huizenschaarste multiplicator lookup'
  Lookup: 'landbeschikbaarheids_multiplicator_voor_huizen_lookup' → 'landbeschikbaarheids multiplicator voor huizen lookup'
  Metadata fixed: author, source, url

**Warnings:**
  WARNING: Auxiliary 'slummultiplicator voor migratie' has no unit
  WARNING: Auxiliary 'leegstandsgraad van huizen' has no unit
  WARNING: Auxiliary 'effect schaarste landgebruik op intensiviteit bedrijfsgebouwen' has no unit
  WARNING: Auxiliary 'extra afbraakgraad' has no unit
  WARNING: Auxiliary 'sterftes' has no unit
  WARNING: Auxiliary 'werkloosheidsgraad' has no unit
  WARNING: Auxiliary 'switch jobs' has no unit
  WARNING: Auxiliary 'switch leegstandseffect' has no unit

### ecology\bluefin-tuna-overfishing.json [TU Delft]

**Changes:**
  Stock: 'Tuna_Biomass' → 'Tuna Biomass'
  Stock: 'Official_Number_of_Tuna_Ships' → 'Official Number of Tuna Ships'
  Flow: 'Tuna_Biomass_net_flow' → 'Tuna Biomass net flow'
  Flow: 'Official_Number_of_Tuna_Ships_net_flow' → 'Official Number of Tuna Ships net flow'
  Aux: 'fraction_tuna_catch' → 'fraction tuna catch'
  Aux: 'perceived_state_of_tuna_fishery' → 'perceived state of tuna fishery'
  Aux: 'growth_existing_tuna_biomass' → 'growth existing tuna biomass'
  Aux: 'illegal_tuna_ships' → 'illegal tuna ships'
  Aux: 'most_recent_perception_concerning_tuna_fishery' → 'most recent perception concerning tuna fishery'
  Aux: 'natural_deaths' → 'natural deaths'
  Aux: 'net_increase_official_number_of_tuna_ships' → 'net increase official number of tuna ships'
  Aux: 'tuna_recruitment' → 'tuna recruitment'
  Aux: 'tuna_catch' → 'tuna catch'
  Aux: 'total_number_of_tuna_ships' → 'total number of tuna ships'
  Aux: 'ratio_of_biomass_to_unfished_biomass' → 'ratio of biomass to unfished biomass'
  Aux: 'proposed_change_in_the_number_of_tuna_ships' → 'proposed change in the number of tuna ships'
  Aux: 'average_efficiency_tuna_ships' → 'average efficiency tuna ships'
  Aux: 'implementation_time_delay' → 'implementation time delay'
  Aux: 'initial_state_tuna_fishery' → 'initial state tuna fishery'
  Aux: 'normal_death_rate' → 'normal death rate'
  Aux: 'unfished_biomass_1990' → 'unfished biomass 1990'
  Aux: 'tuna_recruitment_rate' → 'tuna recruitment rate'
  Aux: 'delay_time' → 'delay time'
  Aux: 'tuna_growth_rate' → 'tuna growth rate'
  Lookup: 'effect_lookup' → 'effect lookup'
  Lookup: 'change_in_tuna_perception_lookup' → 'change in tuna perception lookup'

### ecology\ecological-overshoot.json [TU Delft]

**Changes:**
  Stock: 'renewable_resources' → 'renewable resources'
  Flow: 'renewable_resources_net_flow' → 'renewable resources net flow'
  Flow: 'population_net_flow' → 'population net flow'
  Aux: 'resource_availability_dependent_lifetime' → 'resource availability dependent lifetime'
  Aux: 'resource_use' → 'resource use'
  Aux: 'per_capita_renewable_resource_availability' → 'per capita renewable resource availability'
  Aux: 'rapid_resource_depletion_time' → 'rapid resource depletion time'
  Aux: 'INITIAL_RESOURCES' → 'INITIAL RESOURCES'
  Aux: 'INITIAL_CONSUMERS' → 'INITIAL CONSUMERS'
  Aux: 'regeneration_rate' → 'regeneration rate'
  Aux: 'normal_birth_rate' → 'normal birth rate'
  Aux: 'minimum_regeneration_rate' → 'minimum regeneration rate'
  Aux: 'carrying_capacity' → 'carrying capacity'
  Aux: 'renewable_resource_consumption_per_capita' → 'renewable resource consumption per capita'
  Aux: 'normal_lifetime' → 'normal lifetime'

**Warnings:**
  WARNING: Auxiliary 'resource availability dependent lifetime' has no unit

### ecology\feral-pigs.json [TU Delft]

**Changes:**
  Stock: 'feral_pigs' → 'feral pigs'
  Flow: 'feral_pigs_net_flow' → 'feral pigs net flow'
  Aux: 'catch_per_trap' → 'catch per trap'
  Aux: 'feral_pig_traps' → 'feral pig traps'
  Aux: 'pigs_removed' → 'pigs removed'
  Aux: 'licences_granted_per_year' → 'licences granted per year'
  Aux: 'litter_per_sow' → 'litter per sow'
  Aux: 'piglet_per_litter' → 'piglet per litter'
  Aux: 'traps_per_license' → 'traps per license'
  Metadata fixed: author, source, url

### ecology\kaibab-deer.json [TU Delft]

**Changes:**
  Stock: 'Deer_Population' → 'Deer Population'
  Flow: 'Deer_Population_net_flow' → 'Deer Population net flow'
  Aux: 'Deer_Killed_per_Predator' → 'Deer Killed per Predator'
  Aux: 'Deer_Predation_Rate' → 'Deer Predation Rate'
  Aux: 'Deer_Density' → 'Deer Density'
  Aux: 'Deer_Net_Growth_Rate' → 'Deer Net Growth Rate'
  Aux: 'Initial_Deer_Density' → 'Initial Deer Density'
  Aux: 'Growth_Rate_Factor' → 'Growth Rate Factor'
  Aux: 'Predator_Population' → 'Predator Population'
  Lookup: 'Deer_Killed_per_Predator_lookup' → 'Deer Killed per Predator lookup'

### ecology\muskrat-plague.json [TU Delft]

**Changes:**
  Stock: 'Muskrat_population' → 'Muskrat population'
  Flow: 'Muskrat_population_net_flow' → 'Muskrat population net flow'
  Aux: 'Muskrats_caught' → 'Muskrats caught'
  Aux: 'Muskrats_caught_per_trap' → 'Muskrats caught per trap'
  Aux: 'New_Muskrats' → 'New Muskrats'
  Aux: 'Initial_number_of_muskrats' → 'Initial number of muskrats'
  Aux: 'Number_of_licenses' → 'Number of licenses'
  Aux: 'New_muskrats_rate' → 'New muskrats rate'
  Aux: 'Proportionality_factor' → 'Proportionality factor'
  Aux: 'Traps_per_license' → 'Traps per license'
  Metadata fixed: source, url

### ecology\nutrient-cycle.json

**Changes:**
  Stock: 'Soil_Nitrogen' → 'Soil Nitrogen'
  Stock: 'Plant_Biomass' → 'Plant Biomass'
  Aux: 'Uptake_Rate' → 'Uptake Rate'
  Aux: 'Senescence_Rate' → 'Senescence Rate'
  Aux: 'Decomposition_Rate' → 'Decomposition Rate'

**Warnings:**
  WARNING: No metadata present

### ecology\predator-prey.json

**Changes:**
  Flow: 'Prey_Births' → 'Prey Births'
  Flow: 'Prey_Deaths' → 'Prey Deaths'
  Flow: 'Predator_Births' → 'Predator Births'
  Flow: 'Predator_Deaths' → 'Predator Deaths'
  Aux: 'Prey_Birth_Rate' → 'Prey Birth Rate'
  Aux: 'Predation_Rate' → 'Predation Rate'
  Aux: 'Predator_Efficiency' → 'Predator Efficiency'
  Aux: 'Predator_Death_Rate' → 'Predator Death Rate'

**Warnings:**
  WARNING: No metadata present

### economics\debt-crisis.json [TU Delft]

**Changes:**
  Stock: 'production_plants' → 'production plants'
  Stock: 'profit_sum' → 'profit sum'
  Stock: 'interest_sum' → 'interest sum'
  Flow: 'production_plants_net_flow' → 'production plants net flow'
  Flow: 'debt_net_flow' → 'debt net flow'
  Flow: 'profit_sum_net_flow' → 'profit sum net flow'
  Flow: 'interest_sum_net_flow' → 'interest sum net flow'
  Aux: 'profit_rate' → 'profit rate'
  Aux: 'investment_generation' → 'investment generation'
  Aux: 'CREDIT_FUNCTION' → 'CREDIT FUNCTION'
  Aux: 'net_revenue' → 'net revenue'
  Aux: 'price_factor' → 'price factor'
  Aux: 'debt_decrease' → 'debt decrease'
  Aux: 'debt_increase' → 'debt increase'
  Aux: 'interest_debt' → 'interest debt'
  Aux: 'interest_payment' → 'interest payment'
  Aux: 'INITIAL_PRODUCTION_PLANTS' → 'INITIAL PRODUCTION PLANTS'
  Aux: 'INITIAL_DEBT' → 'INITIAL DEBT'
  Aux: 'INVESTMENT_FRACTION' → 'INVESTMENT FRACTION'
  Aux: 'INVESTMENT_EFFECTIVENESS' → 'INVESTMENT EFFECTIVENESS'
  Aux: 'CREDIT_AMOUNT' → 'CREDIT AMOUNT'
  Aux: 'DEBT_REPAYMENT_FRACTION' → 'DEBT REPAYMENT FRACTION'
  Aux: 'SPECIFIC_PRODUCTION' → 'SPECIFIC PRODUCTION'
  Aux: 'DETERIORATION_RATE' → 'DETERIORATION RATE'
  Aux: 'INTEREST_RATE' → 'INTEREST RATE'
  Lookup: 'CREDIT_FUNCTION_lookup' → 'CREDIT FUNCTION lookup'
  Lookup: 'price_factor_lookup' → 'price factor lookup'
  Metadata fixed: author, source, url

### economics\globalization.json [TU Delft]

**Changes:**
  Stock: 'production_capacity' → 'production capacity'
  Stock: 'production_capacity_0' → 'production capacity 0'
  Stock: 'standards_0' → 'standards 0'
  Flow: 'production_capacity_net_flow' → 'production capacity net flow'
  Flow: 'production_capacity_0_net_flow' → 'production capacity 0 net flow'
  Flow: 'standards_net_flow' → 'standards net flow'
  Flow: 'standards_0_net_flow' → 'standards 0 net flow'
  Aux: 'progress_function' → 'progress function'
  Aux: 'progress_function_0' → 'progress function 0'
  Aux: 'investment_function_0' → 'investment function 0'
  Aux: 'investment_0' → 'investment 0'
  Aux: 'inland_price' → 'inland price'
  Aux: 'inland_price_0' → 'inland price 0'
  Aux: 'progress_investment' → 'progress investment'
  Aux: 'progress_investment_0' → 'progress investment 0'
  Aux: 'subsidy_0' → 'subsidy 0'
  Aux: 'investment_function' → 'investment function'
  Aux: 'customs_0' → 'customs 0'
  Aux: 'progress_0' → 'progress 0'
  Aux: 'price_ratio_domestic_vs_imported_product' → 'price ratio domestic vs imported product'
  Aux: 'supply_0' → 'supply 0'
  Aux: 'production_costs' → 'production costs'
  Aux: 'production_costs_0' → 'production costs 0'
  Aux: 'purchase_decision' → 'purchase decision'
  Aux: 'purchase_decision_0' → 'purchase decision 0'
  Aux: 'demand_0' → 'demand 0'
  Aux: 'price_ratio_domestic_vs_imported_product_0' → 'price ratio domestic vs imported product 0'
  Aux: 'product_costs' → 'product costs'
  Aux: 'product_costs_0' → 'product costs 0'
  Aux: 'product_price' → 'product price'
  Aux: 'product_price_0' → 'product price 0'
  Aux: 'deterioration_0' → 'deterioration 0'
  Aux: 'surplus_0' → 'surplus 0'
  Aux: 'depreciation_0' → 'depreciation 0'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'CUSTOMS_DUTY_0' → 'CUSTOMS DUTY 0'
  Aux: 'INVESTMENT_FACTOR_0' → 'INVESTMENT FACTOR 0'
  Aux: 'EXPORT_SUBSIDY' → 'EXPORT SUBSIDY'
  Aux: 'PROGRESS_FACTOR' → 'PROGRESS FACTOR'
  Aux: 'PROGRESS_FACTOR_0' → 'PROGRESS FACTOR 0'
  Aux: 'CUSTOMS_DUTY' → 'CUSTOMS DUTY'
  Aux: 'REFERENCE_PRICE_0' → 'REFERENCE PRICE 0'
  Aux: 'INVESTMENT_RATE' → 'INVESTMENT RATE'
  Aux: 'INVESTMENT_RATE_0' → 'INVESTMENT RATE 0'
  Aux: 'REFERENCE_PRICE' → 'REFERENCE PRICE'
  Aux: 'EXPORT_SUBSIDY_0' → 'EXPORT SUBSIDY 0'
  Aux: 'INVESTMENT_FACTOR' → 'INVESTMENT FACTOR'
  Aux: 'MARKET_VOLUME' → 'MARKET VOLUME'
  Aux: 'MARKET_VOLUME_0' → 'MARKET VOLUME 0'
  Aux: 'PRODUCTION_RATE' → 'PRODUCTION RATE'
  Aux: 'PRODUCTION_RATE_0' → 'PRODUCTION RATE 0'
  Aux: 'RESOURCE_COSTS' → 'RESOURCE COSTS'
  Aux: 'RESOURCE_COSTS_0' → 'RESOURCE COSTS 0'
  Aux: 'DETERIORATION_RATE' → 'DETERIORATION RATE'
  Aux: 'DETERIORATION_RATE_0' → 'DETERIORATION RATE 0'
  Aux: 'STANDARD_FACTOR' → 'STANDARD FACTOR'
  Aux: 'STANDARD_FACTOR_0' → 'STANDARD FACTOR 0'
  Aux: 'TAX_RATE' → 'TAX RATE'
  Aux: 'TAX_RATE_0' → 'TAX RATE 0'
  Aux: 'DEPRECIATION_RATE' → 'DEPRECIATION RATE'
  Aux: 'DEPRECIATION_RATE_0' → 'DEPRECIATION RATE 0'
  Lookup: 'purchase_decision_lookup' → 'purchase decision lookup'
  Lookup: 'purchase_decision_0_lookup' → 'purchase decision 0 lookup'
  Metadata fixed: author, source, url

### economics\housing-market-crisis.json [TU Delft]

**Changes:**
  Stock: 'cumulative_inflation' → 'cumulative inflation'
  Stock: 'in_planning_and_under_construction' → 'in planning and under construction'
  Stock: 'new_houses' → 'new houses'
  Stock: 'old_houses' → 'old houses'
  Flow: 'cumulative_inflation_net_flow' → 'cumulative inflation net flow'
  Flow: 'in_planning_and_under_construction_net_flow' → 'in planning and under construction net flow'
  Flow: 'new_houses_net_flow' → 'new houses net flow'
  Flow: 'old_houses_net_flow' → 'old houses net flow'
  Aux: 'aging_of_houses' → 'aging of houses'
  Aux: 'average_house_prices' → 'average house prices'
  Aux: 'average_house_spending_capacity_per_household' → 'average house spending capacity per household'
  Aux: 'average_salary_per_household' → 'average salary per household'
  Aux: 'construction_of_new_houses' → 'construction of new houses'
  Aux: 'demolition_multiplicator' → 'demolition multiplicator'
  Aux: 'demolition_of_old_houses' → 'demolition of old houses'
  Aux: 'expected_effective_workforce' → 'expected effective workforce'
  Aux: 'housing_scarcity_ratio' → 'housing scarcity ratio'
  Aux: 'in_planning_and_construction' → 'in planning and construction'
  Aux: 'loan_risk' → 'loan risk'
  Aux: 'normal_planning_and_construction_time' → 'normal planning and construction time'
  Aux: 'normal_salary_lending_multiplier' → 'normal salary lending multiplier'
  Aux: 'profitability_multiplier' → 'profitability multiplier'
  Aux: 'profitability_of_new_house_construction' → 'profitability of new house construction'
  Aux: 'housing_gap' → 'housing gap'
  Aux: 'salary_lending_multiplier' → 'salary lending multiplier'
  Aux: 'total_construction_cost_new_houses' → 'total construction cost new houses'
  Aux: 'total_expected_supply_of_houses' → 'total expected supply of houses'
  Aux: 'expected_households' → 'expected households'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: '_1985_construction_cost_new_houses' → ' 1985 construction cost new houses'
  Aux: 'average_lifetime_old_houses' → 'average lifetime old houses'
  Aux: 'houses_per_household' → 'houses per household'
  Aux: 'expected_inflation_rate' → 'expected inflation rate'
  Aux: 'ini_in_planning_and_under_construction' → 'ini in planning and under construction'
  Aux: 'initial_average_salary' → 'initial average salary'
  Aux: 'lifetime_of_new_houses' → 'lifetime of new houses'
  Aux: 'new_house_premium' → 'new house premium'
  Lookup: 'expected_effective_workforce_lookup' → 'expected effective workforce lookup'
  Lookup: 'normal_planning_and_construction_time_lookup' → 'normal planning and construction time lookup'
  Lookup: 'normal_salary_lending_multiplier_lookup' → 'normal salary lending multiplier lookup'
  Lookup: 'profitability_multiplier_lookup' → 'profitability multiplier lookup'
  Lookup: 'expected_households_lookup' → 'expected households lookup'
  Lookup: 'uncertainty_lookup' → 'uncertainty lookup'
  Metadata fixed: author, source, url

**Warnings:**
  WARNING: Stock 'cumulative inflation' has no unit
  WARNING: Stock 'in planning and under construction' has no unit
  WARNING: Auxiliary 'aging of houses' has no unit
  WARNING: Auxiliary 'demolition multiplicator' has no unit
  WARNING: Auxiliary 'expected effective workforce' has no unit
  WARNING: Auxiliary 'in planning and construction' has no unit
  WARNING: Auxiliary 'loan risk' has no unit
  WARNING: Auxiliary 'uncertainty' has no unit
  WARNING: Auxiliary 'expected inflation rate' has no unit
  WARNING: Auxiliary 'new house premium' has no unit

### economics\real-estate-boom.json [TU Delft]

**Changes:**
  Stock: 'REU_under_construction' → 'REU under construction'
  Stock: 'REU_supply' → 'REU supply'
  Flow: 'REU_under_construction_net_flow' → 'REU under construction net flow'
  Flow: 'immigrants_net_flow' → 'immigrants net flow'
  Flow: 'locals_net_flow' → 'locals net flow'
  Flow: 'REU_supply_net_flow' → 'REU supply net flow'
  Aux: 'relative_attractiveness_to_immigrate' → 'relative attractiveness to immigrate'
  Aux: 'workforce_emigration' → 'workforce emigration'
  Aux: 'REU_price' → 'REU price'
  Aux: 'labour_shortage' → 'labour shortage'
  Aux: 'avg_salary_immigrants' → 'avg salary immigrants'
  Aux: 'expected_new_REU_due_to_immigration' → 'expected new REU due to immigration'
  Aux: 'expected_REU_shortage' → 'expected REU shortage'
  Aux: 'immigrants_previous_period' → 'immigrants previous period'
  Aux: 'workforce_immigration' → 'workforce immigration'
  Aux: 'immigrant_integration' → 'immigrant integration'
  Aux: 'labour_demand' → 'labour demand'
  Aux: 'new_REU_plans_approved' → 'new REU plans approved'
  Aux: 'normal_cost_REU' → 'normal cost REU'
  Aux: 'REU_shortage' → 'REU shortage'
  Aux: 'REU_commissioning' → 'REU commissioning'
  Aux: 'REU_demand' → 'REU demand'
  Aux: 'REU_demand_per_person' → 'REU demand per person'
  Aux: 'REU_demolition' → 'REU demolition'
  Aux: 'avg_REU_approval_time' → 'avg REU approval time'
  Aux: 'avg_emigration_time' → 'avg emigration time'
  Aux: 'avg_immigration_time' → 'avg immigration time'
  Aux: 'avg_lifetime_REU' → 'avg lifetime REU'
  Aux: 'Al_Maktoum_exogenous_investment_rate' → 'Al Maktoum exogenous investment rate'
  Aux: 'immigration_multiplication_factor' → 'immigration multiplication factor'
  Aux: 'immigrant_integration_rate' → 'immigrant integration rate'
  Aux: 'normal_salary_immigrants' → 'normal salary immigrants'
  Aux: 'REU_construction_time' → 'REU construction time'
  Aux: 'workers_per_REU_under_construction' → 'workers per REU under construction'
  Lookup: 'REU_shortage_price_effect' → 'REU shortage price effect'
  Lookup: 'REU_demand_per_person_lookup' → 'REU demand per person lookup'
  Metadata fixed: author, source, url

### epidemiology\cholera.json [TU Delft]

**Changes:**
  Stock: 'cumulative_cholera_cases' → 'cumulative cholera cases'
  Stock: 'cumulative_cholera_deaths' → 'cumulative cholera deaths'
  Stock: 'recently_infected_population' → 'recently infected population'
  Stock: 'recovered_temporarily_immune_population' → 'recovered temporarily immune population'
  Stock: 'mildly_infected_population' → 'mildly infected population'
  Stock: 'heavily_infected_population' → 'heavily infected population'
  Stock: 'susceptible_population' → 'susceptible population'
  Flow: 'cumulative_cholera_cases_net_flow' → 'cumulative cholera cases net flow'
  Flow: 'cumulative_cholera_deaths_net_flow' → 'cumulative cholera deaths net flow'
  Flow: 'recently_infected_population_net_flow' → 'recently infected population net flow'
  Flow: 'recovered_temporarily_immune_population_net_flow' → 'recovered temporarily immune population net flow'
  Flow: 'mildly_infected_population_net_flow' → 'mildly infected population net flow'
  Flow: 'heavily_infected_population_net_flow' → 'heavily infected population net flow'
  Flow: 'susceptible_population_net_flow' → 'susceptible population net flow'
  Aux: 'recorded_cholera_cases' → 'recorded cholera cases'
  Aux: 'effect_of_prevention_and_sanitation_on_the_indirect_degree_of_infection' → 'effect of prevention and sanitation on the indirect degree of infection'
  Aux: 'level_of_prevention' → 'level of prevention'
  Aux: 'fraction_of_infected' → 'fraction of infected'
  Aux: 'smoothed_fraction_of_contaminated_water' → 'smoothed fraction of contaminated water'
  Aux: 'indirect_degree_of_infection' → 'indirect degree of infection'
  Aux: 'cholera_deaths' → 'cholera deaths'
  Aux: 'effect_of_the_fraction_of_infected_on_the_fraction_of_contaminated_water' → 'effect of the fraction of infected on the fraction of contaminated water'
  Aux: 'effect_of_the_average_state_of_health_services_on_the_fraction_of_cholera_deaths' → 'effect of the average state of health services on the fraction of cholera deaths'
  Aux: 'average_immunity_period' → 'average immunity period'
  Aux: 'average_state_of_health_services' → 'average state of health services'
  Aux: 'recovered_from_heavy_infection' → 'recovered from heavy infection'
  Aux: 'recovered_to_susceptible_by_loss_of_immunity' → 'recovered to susceptible by loss of immunity'
  Aux: 'cholera_infections' → 'cholera infections'
  Aux: 'recovered_from_mild_infection' → 'recovered from mild infection'
  Aux: 'condition_of_the_sanitary_infrastructure' → 'condition of the sanitary infrastructure'
  Aux: 'mildly_infected' → 'mildly infected'
  Aux: 'heavily_infected' → 'heavily infected'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'connectedness_of_aquifers' → 'connectedness of aquifers'
  Aux: 'effect_of_the_average_health_condition_on_the_fraction_of_mildly_infected_of_all_infected' → 'effect of the average health condition on the fraction of mildly infected of all infected'
  Aux: 'average_incubation_time' → 'average incubation time'
  Aux: 'average_duration_of_illness' → 'average duration of illness'
  Lookup: 'effect_of_prevention_and_sanitation_on_the_indirect_degree_of_infection_lookup' → 'effect of prevention and sanitation on the indirect degree of infection lookup'
  Lookup: 'effect_of_the_fraction_of_infected_on_the_fraction_of_contaminated_water_lookup' → 'effect of the fraction of infected on the fraction of contaminated water lookup'
  Lookup: 'effect_of_the_average_state_of_health_services_on_the_fraction_of_cholera_deaths_lookup' → 'effect of the average state of health services on the fraction of cholera deaths lookup'
  Metadata fixed: author, source, url

**Warnings:**
  WARNING: Stock 'cumulative cholera cases' has no unit
  WARNING: Stock 'recently infected population' has no unit
  WARNING: Stock 'susceptible population' has no unit
  WARNING: Auxiliary 'recorded cholera cases' has no unit
  WARNING: Auxiliary 'effect of prevention and sanitation on the indirect degree of infection' has no unit
  WARNING: Auxiliary 'smoothed fraction of contaminated water' has no unit
  WARNING: Auxiliary 'effect of the fraction of infected on the fraction of contaminated water' has no unit
  WARNING: Auxiliary 'effect of the average state of health services on the fraction of cholera deaths' has no unit
  WARNING: Auxiliary 'average state of health services' has no unit
  WARNING: Auxiliary 'recovered to susceptible by loss of immunity' has no unit
  WARNING: Auxiliary 'heavily infected' has no unit
  WARNING: Auxiliary 'connectedness of aquifers' has no unit
  WARNING: Auxiliary 'effect of the average health condition on the fraction of mildly infected of all infected' has no unit

### epidemiology\flu-two-regions.json [TU Delft]

**Changes:**
  Stock: 'infected_population_region_1' → 'infected population region 1'
  Stock: 'infected_population_region_2' → 'infected population region 2'
  Stock: 'recovered_population_region_1' → 'recovered population region 1'
  Stock: 'recovered_population_region_2' → 'recovered population region 2'
  Stock: 'susceptible_population_region_1' → 'susceptible population region 1'
  Stock: 'susceptible_population_region_2' → 'susceptible population region 2'
  Stock: 'immune_population_region_1' → 'immune population region 1'
  Stock: 'immune_population_region_2' → 'immune population region 2'
  Flow: 'infected_population_region_1_net_flow' → 'infected population region 1 net flow'
  Flow: 'infected_population_region_2_net_flow' → 'infected population region 2 net flow'
  Flow: 'recovered_population_region_1_net_flow' → 'recovered population region 1 net flow'
  Flow: 'recovered_population_region_2_net_flow' → 'recovered population region 2 net flow'
  Flow: 'susceptible_population_region_1_net_flow' → 'susceptible population region 1 net flow'
  Flow: 'susceptible_population_region_2_net_flow' → 'susceptible population region 2 net flow'
  Flow: 'immune_population_region_1_net_flow' → 'immune population region 1 net flow'
  Flow: 'immune_population_region_2_net_flow' → 'immune population region 2 net flow'
  Aux: 'normal_immune_population_fraction_region_1' → 'normal immune population fraction region 1'
  Aux: 'recoveries_region_2' → 'recoveries region 2'
  Aux: 'susceptible_to_immune_population_flow_region_2' → 'susceptible to immune population flow region 2'
  Aux: 'normal_immune_population_fraction_region_2' → 'normal immune population fraction region 2'
  Aux: 'normal_immune_population_region_2' → 'normal immune population region 2'
  Aux: 'recoveries_region_1' → 'recoveries region 1'
  Aux: 'infection_fraction_region_1' → 'infection fraction region 1'
  Aux: 'infection_fraction_region_2' → 'infection fraction region 2'
  Aux: 'total_population_region_1' → 'total population region 1'
  Aux: 'total_population_region_2' → 'total population region 2'
  Aux: 'infections_region_1' → 'infections region 1'
  Aux: 'infections_region_2' → 'infections region 2'
  Aux: 'susceptible_to_immune_population_flow_region_1' → 'susceptible to immune population flow region 1'
  Aux: 'normal_immune_population_region_1' → 'normal immune population region 1'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'initial_value_susceptible_population_region_2' → 'initial value susceptible population region 2'
  Aux: 'initial_value_infected_population_region_1' → 'initial value infected population region 1'
  Aux: 'initial_value_immune_population_region_1' → 'initial value immune population region 1'
  Aux: 'initial_value_immune_population_region_2' → 'initial value immune population region 2'
  Aux: 'initial_value_susceptible_population_region_1' → 'initial value susceptible population region 1'
  Aux: 'initial_value_infected_population_region_2' → 'initial value infected population region 2'
  Aux: 'initial_value_recovered_population_region_1' → 'initial value recovered population region 1'
  Aux: 'initial_value_recovered_population_region_2' → 'initial value recovered population region 2'
  Aux: 'interregional_contact_rate' → 'interregional contact rate'
  Aux: 'susceptible_to_immune_population_delay_time_region_2' → 'susceptible to immune population delay time region 2'
  Aux: 'contact_rate_region_1' → 'contact rate region 1'
  Aux: 'contact_rate_region_2' → 'contact rate region 2'
  Aux: 'infection_rate_region_1' → 'infection rate region 1'
  Aux: 'infection_rate_region_2' → 'infection rate region 2'
  Aux: 'susceptible_to_immune_population_delay_time_region_1' → 'susceptible to immune population delay time region 1'
  Aux: 'recovering_time' → 'recovering time'
  Lookup: 'normal_immune_population_fraction_region_1_lookup' → 'normal immune population fraction region 1 lookup'
  Lookup: 'normal_immune_population_fraction_region_2_lookup' → 'normal immune population fraction region 2 lookup'
  Metadata fixed: author, source, url

### epidemiology\pneumonic-plague.json [TU Delft]

**Changes:**
  Stock: 'susceptible_population' → 'susceptible population'
  Stock: 'infected_population' → 'infected population'
  Stock: 'recovering_population' → 'recovering population'
  Stock: 'deceased_population' → 'deceased population'
  Flow: 'susceptible_population_net_flow' → 'susceptible population net flow'
  Flow: 'infected_population_net_flow' → 'infected population net flow'
  Flow: 'recovering_population_net_flow' → 'recovering population net flow'
  Flow: 'deceased_population_net_flow' → 'deceased population net flow'
  Aux: 'infected_fraction' → 'infected fraction'
  Aux: 'total_population' → 'total population'
  Aux: 'contact_rate' → 'contact rate'
  Aux: 'antibiotics_coverage_of_the_population' → 'antibiotics coverage of the population'
  Aux: 'recovery_time' → 'recovery time'
  Aux: 'infection_ratio' → 'infection ratio'
  Aux: 'fatality_ratio' → 'fatality ratio'
  Aux: 'initial_total_population' → 'initial total population'
  Lookup: 'fatality_ratio_lookup' → 'fatality ratio lookup'
  Metadata fixed: author, source, url

### epidemiology\sir-epidemic.json

**Changes:**
  Aux: 'Contact_Rate' → 'Contact Rate'
  Aux: 'Recovery_Rate' → 'Recovery Rate'

**Warnings:**
  WARNING: No metadata present

### introductory\bathtub.json

**Changes:**
  Stock: 'Water_in_Tub' → 'Water in Tub'
  Aux: 'Outflow_Rate' → 'Outflow Rate'
  Aux: 'Inflow_Rate' → 'Inflow Rate'

**Warnings:**
  WARNING: No metadata present

### introductory\coffee-cooling.json

**Changes:**
  Stock: 'Coffee_Temperature' → 'Coffee Temperature'
  Aux: 'Room_Temperature' → 'Room Temperature'
  Aux: 'Cooling_Rate' → 'Cooling Rate'

**Warnings:**
  WARNING: No metadata present

### introductory\exponential-growth.json

**Changes:**
  Aux: 'Birth_Rate' → 'Birth Rate'
  Aux: 'Death_Rate' → 'Death Rate'

**Warnings:**
  WARNING: No metadata present

### introductory\goal-seeking.json

**Changes:**
  Aux: 'Adjustment_Time' → 'Adjustment Time'

**Warnings:**
  WARNING: No metadata present

### management\competition-faculty.json

**Warnings:**
  WARNING: Metadata missing 'author'
  WARNING: Metadata missing 'source'

### management\managing-faculty.json [TU Delft]

**Changes:**
  Flow: 'money_net_flow' → 'money net flow'
  Flow: 'professors_net_flow' → 'professors net flow'
  Aux: 'desired_number_of_professors' → 'desired number of professors'
  Aux: 'earnings_from_papers' → 'earnings from papers'
  Aux: 'money_spent_on_salaries' → 'money spent on salaries'
  Aux: 'new_professors_hired' → 'new professors hired'
  Aux: 'number_of_papers_published' → 'number of papers published'
  Aux: 'subsidy_and_earnings_from_papers' → 'subsidy and earnings from papers'
  Aux: 'professors_leaving' → 'professors leaving'
  Aux: 'percentage_of_leavers' → 'percentage of leavers'
  Aux: 'teaching_fee' → 'teaching fee'
  Aux: 'average_hiring_time' → 'average hiring time'
  Aux: 'average_professor_salary' → 'average professor salary'
  Aux: 'earnings_per_published_paper' → 'earnings per published paper'
  Aux: 'initially_available_money' → 'initially available money'
  Aux: 'number_of_papers_per_professor' → 'number of papers per professor'
  Metadata fixed: author, source, url

### management\project-management.json [TU Delft]

**Changes:**
  Stock: 'remaining_project_tasks' → 'remaining project tasks'
  Stock: 'undiscovered_rework' → 'undiscovered rework'
  Stock: 'cumulative_effort' → 'cumulative effort'
  Stock: 'properly_completed_project_tasks' → 'properly completed project tasks'
  Flow: 'remaining_project_tasks_net_flow' → 'remaining project tasks net flow'
  Flow: 'undiscovered_rework_net_flow' → 'undiscovered rework net flow'
  Flow: 'cumulative_effort_net_flow' → 'cumulative effort net flow'
  Flow: 'properly_completed_project_tasks_net_flow' → 'properly completed project tasks net flow'
  Flow: 'workforce_net_flow' → 'workforce net flow'
  Aux: 'fraction_personnel_for_testing' → 'fraction personnel for testing'
  Aux: 'average_quality_of_completed_project_tasks' → 'average quality of completed project tasks'
  Aux: 'productivity_of_testing' → 'productivity of testing'
  Aux: 'perceived_time_remaining' → 'perceived time remaining'
  Aux: 'additional_cumulative_effort' → 'additional cumulative effort'
  Aux: 'reported_fraction_detection_undiscovered_rework' → 'reported fraction detection undiscovered rework'
  Aux: 'fraction_undiscovered_rework' → 'fraction undiscovered rework'
  Aux: 'project_personnel' → 'project personnel'
  Aux: 'detecting_undiscovered_rework' → 'detecting undiscovered rework'
  Aux: 'testing_personnel' → 'testing personnel'
  Aux: 'gross_productivity_of_project_personnel' → 'gross productivity of project personnel'
  Aux: 'perceived_effort_remaining' → 'perceived effort remaining'
  Aux: 'poor_completion_of_project_tasks' → 'poor completion of project tasks'
  Aux: 'proper_completion_of_project_tasks' → 'proper completion of project tasks'
  Aux: 'net_hiring_of_personnel' → 'net hiring of personnel'
  Aux: 'perceived_cumulative_progress' → 'perceived cumulative progress'
  Aux: 'perceived_fraction_completed' → 'perceived fraction completed'
  Aux: 'perceived_productivity' → 'perceived productivity'
  Aux: 'desired_workforce' → 'desired workforce'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'initial_project_time_remaining' → 'initial project time remaining'
  Aux: 'maximum_productivity_of_testing' → 'maximum productivity of testing'
  Aux: 'time_to_adapt_workforce' → 'time to adapt workforce'
  Aux: 'time_to_adapt_the_project_schedule' → 'time to adapt the project schedule'
  Aux: 'fraction_properly_completed' → 'fraction properly completed'
  Aux: 'initial_number_of_project_tasks' → 'initial number of project tasks'
  Lookup: 'fraction_personnel_for_testing_lookup' → 'fraction personnel for testing lookup'
  Lookup: 'gross_productivity_of_project_personnel_lookup' → 'gross productivity of project personnel lookup'
  Metadata fixed: author, source, url

### policy\cocaine-flow.json [TU Delft]

**Changes:**
  Flow: 'Cocaine_net_flow' → 'Cocaine net flow'
  Aux: 'confiscation_rate' → 'confiscation rate'

### policy\deradicalization.json [TU Delft]

**Changes:**
  Stock: 'convinced_citizens' → 'convinced citizens'
  Stock: 'unconvinced_citizens' → 'unconvinced citizens'
  Stock: 'animal_distress' → 'animal distress'
  Flow: 'extremists_net_flow' → 'extremists net flow'
  Flow: 'convinced_citizens_net_flow' → 'convinced citizens net flow'
  Flow: 'activists_net_flow' → 'activists net flow'
  Flow: 'unconvinced_citizens_net_flow' → 'unconvinced citizens net flow'
  Flow: 'animal_distress_net_flow' → 'animal distress net flow'
  Aux: 'visibility_of_the_problem' → 'visibility of the problem'
  Aux: 'frustration_of_convinced_citizens' → 'frustration of convinced citizens'
  Aux: 'nonradical_action_level' → 'nonradical action level'
  Aux: 'fraction_of_convinced_citizens' → 'fraction of convinced citizens'
  Aux: 'radical_action_level' → 'radical action level'
  Aux: 'activists_2_extremists' → 'activists 2 extremists'
  Aux: 'convinced_2_activist' → 'convinced 2 activist'
  Aux: 'contact_rate_of_convinced_citizens' → 'contact rate of convinced citizens'
  Aux: 'potential_number_of_extremists' → 'potential number of extremists'
  Aux: 'potential_number_of_activitsts' → 'potential number of activitsts'
  Aux: 'frustration_due_to_inertia' → 'frustration due to inertia'
  Aux: 'frustration_due_to_marginalization' → 'frustration due to marginalization'
  Aux: 'net_increase_of_animal_distress' → 'net increase of animal distress'
  Aux: 'perceived_animal_distress' → 'perceived animal distress'
  Aux: 'problem_symptoms' → 'problem symptoms'
  Aux: 'societal_acceptation_threshold_with_regard_to_animal_distress' → 'societal acceptation threshold with regard to animal distress'
  Aux: 'rate_of_decrease_through_societal_change' → 'rate of decrease through societal change'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'commitment_of_radical_individuals_to_radical_action' → 'commitment of radical individuals to radical action'
  Aux: 'potential_fraction_of_extremists' → 'potential fraction of extremists'
  Aux: 'average_time_to_change_states' → 'average time to change states'
  Aux: 'ini_extremists' → 'ini extremists'
  Aux: 'ini_activists' → 'ini activists'
  Aux: 'potential_fraction_of_activists' → 'potential fraction of activists'
  Aux: 'persuasiveness_of_nonradical_actions' → 'persuasiveness of nonradical actions'
  Aux: 'citizens_that_cannot_be_convinced' → 'citizens that cannot be convinced'
  Aux: 'ini_convinced' → 'ini convinced'
  Aux: 'ini_unconvinced' → 'ini unconvinced'
  Aux: 'initial_value_of_underlying_phenomenon' → 'initial value of underlying phenomenon'
  Aux: 'exogenous_rate_of_increase_of_animal_distress' → 'exogenous rate of increase of animal distress'
  Aux: 'maximum_attainable_rate_of_decrease_through_societal_change' → 'maximum attainable rate of decrease through societal change'
  Aux: 'normal_contact_rate_of_convinced_citizens' → 'normal contact rate of convinced citizens'
  Aux: 'strength_of_incompatible_selfinterest' → 'strength of incompatible selfinterest'
  Lookup: 'frustration_due_to_marginalization_lookup' → 'frustration due to marginalization lookup'
  Lookup: 'societal_acceptation_threshold_with_regard_to_animal_distress_lookup' → 'societal acceptation threshold with regard to animal distress lookup'
  Metadata fixed: author, source, url

### population\s-shaped-growth.json

**Changes:**
  Aux: 'Max_Birth_Rate' → 'Max Birth Rate'
  Aux: 'Carrying_Capacity' → 'Carrying Capacity'

**Warnings:**
  WARNING: No metadata present

### supply-chain\inventory-oscillation.json [TU Delft]

**Changes:**
  Stock: 'Cars_on_Lot' → 'Cars on Lot'
  Stock: 'Perceived_Sales' → 'Perceived Sales'
  Flow: 'Perception_Adjustment' → 'Perception Adjustment'
  Aux: 'Customer_Demand' → 'Customer Demand'
  Aux: 'Desired_Inventory' → 'Desired Inventory'
  Aux: 'Inventory_Gap' → 'Inventory Gap'
  Aux: 'Orders_to_Factory' → 'Orders to Factory'
  Aux: 'Base_Demand' → 'Base Demand'
  Aux: 'Step_Demand' → 'Step Demand'
  Aux: 'Perception_Delay' → 'Perception Delay'
  Aux: 'Response_Delay' → 'Response Delay'
  Aux: 'Delivery_Delay' → 'Delivery Delay'
  Aux: 'Desired_Inventory_Multiplier' → 'Desired Inventory Multiplier'
  Metadata fixed: author, source, license, url

### supply-chain\supply-chain-bullwhip.json [TU Delft]

**Changes:**
  Flow: 'Inventory_net_flow' → 'Inventory net flow'
  Flow: 'Workforce_net_flow' → 'Workforce net flow'
  Aux: 'target_inventory' → 'target inventory'
  Aux: 'target_production' → 'target production'
  Aux: 'inventory_correction' → 'inventory correction'
  Aux: 'net_hire_rate' → 'net hire rate'
  Aux: 'target_workforce' → 'target workforce'
  Aux: 'inventory_coverage' → 'inventory coverage'
  Aux: 'time_to_correct_inventory' → 'time to correct inventory'
  Aux: 'time_to_adjust_workforce' → 'time to adjust workforce'
  Metadata fixed: author, source, url

### technology\energy-transition.json [TU Delft]

**Changes:**
  Stock: 'installed_capacity_T3' → 'installed capacity T3'
  Stock: 'marginal_cost_new_capacity_T3' → 'marginal cost new capacity T3'
  Stock: 'capacity_under_construction_T3' → 'capacity under construction T3'
  Stock: 'cumulatively_decommissioned_capacity_T3' → 'cumulatively decommissioned capacity T3'
  Stock: 'capacity_under_construction_T1' → 'capacity under construction T1'
  Stock: 'capacity_under_construction_T2' → 'capacity under construction T2'
  Stock: 'cumulatively_decommissioned_capacity_T2' → 'cumulatively decommissioned capacity T2'
  Stock: 'marginal_cost_new_capacity_T2' → 'marginal cost new capacity T2'
  Stock: 'installed_capacity_T2' → 'installed capacity T2'
  Stock: 'marginal_cost_new_capacity_T1' → 'marginal cost new capacity T1'
  Stock: 'cumulatively_decommissioned_capacity_T1' → 'cumulatively decommissioned capacity T1'
  Stock: 'installed_capacity_T1' → 'installed capacity T1'
  Flow: 'installed_capacity_T3_net_flow' → 'installed capacity T3 net flow'
  Flow: 'marginal_cost_new_capacity_T3_net_flow' → 'marginal cost new capacity T3 net flow'
  Flow: 'capacity_under_construction_T3_net_flow' → 'capacity under construction T3 net flow'
  Flow: 'cumulatively_decommissioned_capacity_T3_net_flow' → 'cumulatively decommissioned capacity T3 net flow'
  Flow: 'capacity_under_construction_T1_net_flow' → 'capacity under construction T1 net flow'
  Flow: 'capacity_under_construction_T2_net_flow' → 'capacity under construction T2 net flow'
  Flow: 'cumulatively_decommissioned_capacity_T2_net_flow' → 'cumulatively decommissioned capacity T2 net flow'
  Flow: 'marginal_cost_new_capacity_T2_net_flow' → 'marginal cost new capacity T2 net flow'
  Flow: 'installed_capacity_T2_net_flow' → 'installed capacity T2 net flow'
  Flow: 'marginal_cost_new_capacity_T1_net_flow' → 'marginal cost new capacity T1 net flow'
  Flow: 'cumulatively_decommissioned_capacity_T1_net_flow' → 'cumulatively decommissioned capacity T1 net flow'
  Flow: 'installed_capacity_T1_net_flow' → 'installed capacity T1 net flow'
  Aux: 'marginal_cost_new_capacity_previous_year_T3' → 'marginal cost new capacity previous year T3'
  Aux: 'marginal_cost_new_capacity_previous_year_T2' → 'marginal cost new capacity previous year T2'
  Aux: 'marginal_cost_new_capacity_previous_year_T1' → 'marginal cost new capacity previous year T1'
  Aux: 'sustainable_fraction_of_total_installed_capacity' → 'sustainable fraction of total installed capacity'
  Aux: 'cumulatively_installed_capacity_T3' → 'cumulatively installed capacity T3'
  Aux: 'desired_fraction_new_capacity_T3' → 'desired fraction new capacity T3'
  Aux: 'newly_planned_capacity_T3' → 'newly planned capacity T3'
  Aux: 'cumulatively_installed_capacity_previous_year_T3' → 'cumulatively installed capacity previous year T3'
  Aux: 'commissioning_capacity_T3' → 'commissioning capacity T3'
  Aux: 'decommissioning_capacity_T3' → 'decommissioning capacity T3'
  Aux: 'marginal_cost_capacity_T3' → 'marginal cost capacity T3'
  Aux: 'experience_curve_parameter_T3' → 'experience curve parameter T3'
  Aux: 'cumulatively_installed_capacity_T2' → 'cumulatively installed capacity T2'
  Aux: 'desired_fraction_new_capacity_T2' → 'desired fraction new capacity T2'
  Aux: 'cumulatively_installed_capacity_previous_year_T2' → 'cumulatively installed capacity previous year T2'
  Aux: 'commissioning_capacity_T2' → 'commissioning capacity T2'
  Aux: 'marginal_cost_capacity_T2' → 'marginal cost capacity T2'
  Aux: 'desired_fraction_new_capacity_T1' → 'desired fraction new capacity T1'
  Aux: 'commissioning_capacity_T1' → 'commissioning capacity T1'
  Aux: 'experience_curve_parameter_T2' → 'experience curve parameter T2'
  Aux: 'total_installed_capacity' → 'total installed capacity'
  Aux: 'newly_planned_capacity_T2' → 'newly planned capacity T2'
  Aux: 'decommissioning_capacity_T2' → 'decommissioning capacity T2'
  Aux: 'expected_capacity_required' → 'expected capacity required'
  Aux: 'newly_planned_capacity_T1' → 'newly planned capacity T1'
  Aux: 'expected_new_capacity_to_be_installed' → 'expected new capacity to be installed'
  Aux: 'decommissioning_capacity_T1' → 'decommissioning capacity T1'
  Aux: 'progress_ratio_technology_3' → 'progress ratio technology 3'
  Aux: 'experience_curve_parameter_tech_3' → 'experience curve parameter tech 3'
  Aux: 'cumulatively_installed_capacity_T1' → 'cumulatively installed capacity T1'
  Aux: 'experience_curve_parameter_tech_2' → 'experience curve parameter tech 2'
  Aux: 'cumulatively_installed_capacity_previous_year_T1' → 'cumulatively installed capacity previous year T1'
  Aux: 'lifetime_technology_2' → 'lifetime technology 2'
  Aux: 'progress_ratio_technology_2' → 'progress ratio technology 2'
  Aux: 'marginal_cost_capacity_T1' → 'marginal cost capacity T1'
  Aux: 'initial_capacity_T1' → 'initial capacity T1'
  Aux: 'experience_curve_parameter_T1' → 'experience curve parameter T1'
  Aux: 'progress_ratio_T1' → 'progress ratio T1'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'learning_curve_period' → 'learning curve period'
  Aux: 'average_construction_time_T3' → 'average construction time T3'
  Aux: 'initial_capacity_T3' → 'initial capacity T3'
  Aux: 'initial_capacity_under_construction_T3' → 'initial capacity under construction T3'
  Aux: 'lifetime_techmology_T3' → 'lifetime techmology T3'
  Aux: 'progress_ratio_T3' → 'progress ratio T3'
  Aux: 'initial_cumulatively_decommissioned_capacity_T3' → 'initial cumulatively decommissioned capacity T3'
  Aux: 'initial_marginal_cost_new_capacity_T3' → 'initial marginal cost new capacity T3'
  Aux: 'average_construction_time_T2' → 'average construction time T2'
  Aux: 'initial_capacity_under_construction_T1' → 'initial capacity under construction T1'
  Aux: 'average_construction_time_T1' → 'average construction time T1'
  Aux: 'initial_cumulatively_decommissioned_capacity_T2' → 'initial cumulatively decommissioned capacity T2'
  Aux: 'initial_marginal_cost_new_capacity_T2' → 'initial marginal cost new capacity T2'
  Aux: 'initial_capacity_under_construction_T2' → 'initial capacity under construction T2'
  Aux: 'initial_capacity_T2' → 'initial capacity T2'
  Aux: 'lifetime_techmology_T2' → 'lifetime techmology T2'
  Aux: 'planning_period' → 'planning period'
  Aux: 'progress_ratio_T2' → 'progress ratio T2'
  Aux: 'initial_marginal_cost_new_capacity_T1' → 'initial marginal cost new capacity T1'
  Aux: 'initial_cumulatively_decommissioned_capacity_T1' → 'initial cumulatively decommissioned capacity T1'
  Aux: 'lifetime_techmology_T1' → 'lifetime techmology T1'
  Lookup: 'expected_capacity_required_lookup' → 'expected capacity required lookup'
  Metadata fixed: author, source, url

### technology\evs-lithium-scarcity.json [TU Delft]

**Changes:**
  Stock: 'conventional_vehicles' → 'conventional vehicles'
  Stock: 'electric_vehicles_EV' → 'electric vehicles EV'
  Stock: 'processing_of_lithium_in_products' → 'processing of lithium in products'
  Stock: 'annual_ICT_lithium_demand' → 'annual ICT lithium demand'
  Stock: 'lithium_in_products' → 'lithium in products'
  Stock: 'lithium_in_recycling' → 'lithium in recycling'
  Stock: 'unexploited_lithium_reserves' → 'unexploited lithium reserves'
  Flow: 'conventional_vehicles_net_flow' → 'conventional vehicles net flow'
  Flow: 'electric_vehicles_EV_net_flow' → 'electric vehicles EV net flow'
  Flow: 'processing_of_lithium_in_products_net_flow' → 'processing of lithium in products net flow'
  Flow: 'annual_ICT_lithium_demand_net_flow' → 'annual ICT lithium demand net flow'
  Flow: 'lithium_in_products_net_flow' → 'lithium in products net flow'
  Flow: 'lithium_in_recycling_net_flow' → 'lithium in recycling net flow'
  Flow: 'unexploited_lithium_reserves_net_flow' → 'unexploited lithium reserves net flow'
  Aux: 'EV_batteries_needed' → 'EV batteries needed'
  Aux: 'lithium_exploitation' → 'lithium exploitation'
  Aux: 'lithium_from_recycling_to_reprocessing' → 'lithium from recycling to reprocessing'
  Aux: 'lithium_products_brought_into_use' → 'lithium products brought into use'
  Aux: 'approximated_fraction_of_lithium_use_in_EV_batteries_over_the_total_amount_of_lithium_in_use' → 'approximated fraction of lithium use in EV batteries over the total amount of lithium in use'
  Aux: 'lithium_needed_for_EV_batteries' → 'lithium needed for EV batteries'
  Aux: 'average_lifetime_products_containing_lithium' → 'average lifetime products containing lithium'
  Aux: 'electrification_European_vehicle_fleet' → 'electrification European vehicle fleet'
  Aux: 'total_demand_lithium_to_be_exploited' → 'total demand lithium to be exploited'
  Aux: 'net_increase_annual_ICT_lithium_demand' → 'net increase annual ICT lithium demand'
  Aux: 'intensive_advertisement' → 'intensive advertisement'
  Aux: 'totale_European_vehicle_fleet' → 'totale European vehicle fleet'
  Aux: 'recovery_lithium_through_recycling' → 'recovery lithium through recycling'
  Aux: 'loss_of_nonrecyled_lithium' → 'loss of nonrecyled lithium'
  Aux: 'TIME_STEP' → 'TIME STEP'
  Aux: 'INITIAL_TIME' → 'INITIAL TIME'
  Aux: 'FINAL_TIME' → 'FINAL TIME'
  Aux: 'battery_per_vehicle' → 'battery per vehicle'
  Aux: 'average_recycling_time' → 'average recycling time'
  Aux: 'processing_and_introduction_time' → 'processing and introduction time'
  Aux: 'exploitation_time' → 'exploitation time'
  Aux: 'batteries_per_vehicle' → 'batteries per vehicle'
  Aux: 'average_lifetime_EV_batteries_during_and_after_use_in_EV' → 'average lifetime EV batteries during and after use in EV'
  Aux: 'average_use_time_of_ICT_products_containing_lithium' → 'average use time of ICT products containing lithium'
  Aux: 'average_lifetime_conventional_vehicles' → 'average lifetime conventional vehicles'
  Aux: 'average_lifetime_EV_batteries' → 'average lifetime EV batteries'
  Aux: 'lithium_per_EV_battery' → 'lithium per EV battery'
  Aux: 'growth_rate_lithium_demand_ICT_sector' → 'growth rate lithium demand ICT sector'
  Aux: 'initial_value_lithium_in_products' → 'initial value lithium in products'
  Aux: 'initially_unexploited_lithium_reserves' → 'initially unexploited lithium reserves'
  Aux: 'effective_recycling_fraction_lithium' → 'effective recycling fraction lithium'
  Metadata fixed: author, source, url

### technology\micro-chp-diffusion.json [TU Delft]

**Changes:**
  Stock: 'potential_clients' → 'potential clients'
  Flow: 'clients_net_flow' → 'clients net flow'
  Flow: 'potential_clients_net_flow' → 'potential clients net flow'
  Aux: '_99_per_cent' → ' 99 per cent'
  Aux: 'new_clients' → 'new clients'
  Aux: 'contacts_of_potential_clients_with_clients' → 'contacts of potential clients with clients'
  Aux: 'contacts_with_clients' → 'contacts with clients'
  Aux: 'potential_client_concentration' → 'potential client concentration'
  Aux: 'total_market' → 'total market'
  Aux: 'convincing_degree' → 'convincing degree'
  Aux: 'social_contacts_per_person_per_month' → 'social contacts per person per month'
  Metadata fixed: author, source, url
