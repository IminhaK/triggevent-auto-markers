package iminhak.anabaseios;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.MultiSlotAutoMarkHandler;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.JobSortOverrideSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CalloutRepo(name = "Iminha's P12S", duty = KnownDuty.P12S)
public class P12SP2AutoMarkers extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(P12SP2AutoMarkers.class);

    public P12SP2AutoMarkers(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.state = state;
        this.buffs = buffs;
        String settingKeyBase = "triggers.iminha.p12sp2.";
        groupPrioJobSort = new JobSortSetting(pers, settingKeyBase + "groupsPrio", state);
        pangenesisAmEnable = new BooleanSetting(pers, settingKeyBase + "pangenesis-am.enabled", false);
        pangenesisPrio = new JobSortOverrideSetting(pers, settingKeyBase + "pangenesis-prio-override", state, groupPrioJobSort);
        pangenesisAMSettings = new MultiSlotAutomarkSetting<>(pers, settingKeyBase + "groupsPrio.pangenesis-am-settings", PangenesisAssignment.class, Map.of(
                PangenesisAssignment.Debuff1, MarkerSign.BIND1,
                PangenesisAssignment.Nothing1, MarkerSign.BIND2,
                PangenesisAssignment.Debuff2, MarkerSign.IGNORE1,
                PangenesisAssignment.Nothing2, MarkerSign.IGNORE2,
                PangenesisAssignment.ShortDark, MarkerSign.ATTACK1,
                PangenesisAssignment.LongDark, MarkerSign.ATTACK2,
                PangenesisAssignment.ShortLight, MarkerSign.TRIANGLE,
                PangenesisAssignment.LongLight, MarkerSign.SQUARE
        ));
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> pangenesisAm = SqtTemplates.sq(30_000,
            AbilityCastStart.class, acs -> acs.abilityIdMatches(0x833F),
            (e1, s) -> {
                if(getPangenesisAmEnable().get()) {
                    s.accept(new ClearAutoMarkRequest());
                }
                List<BuffApplied> unstable = new ArrayList<>(2);
                List<BuffApplied> lightShort = new ArrayList<>(1);
                List<BuffApplied> lightLong = new ArrayList<>(1);
                List<BuffApplied> darkShort = new ArrayList<>(1);
                List<BuffApplied> darkLong = new ArrayList<>(1);
                List<XivPlayerCharacter> nothing = new ArrayList<>(getState().getPartyList());
                if(nothing.size() != 8) {
                    log.warn("Party list size is not 8! [{}]", XivEntity.fmtShortList(nothing));
                }
                //E09 Unstable x 2
                //DF8 Light x 2
                //DF9 Dark x 2
                //Long = 20s (Check > 19s)
                //Short = 16s (else)
                s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xE09));
                s.waitMs(100);
                //sort debuffs
                getBuffs().getBuffs().forEach(ba -> {
                    if (ba.buffIdMatches(0xE09) && ba.getStacks() == 1) {
                        unstable.add(ba);
                    } else if (ba.buffIdMatches(0xDF8)) {
                        if(ba.getInitialDuration().toSeconds() > 19) {
                            lightLong.add(ba);
                        } else {
                            lightShort.add(ba);
                        }
                    } else if (ba.buffIdMatches(0xDF9)) {
                        if(ba.getInitialDuration().toSeconds() > 19) {
                            darkLong.add(ba);
                        } else {
                            darkShort.add(ba);
                        }
                    } else {
                        return;
                    }
                    XivCombatant target = ba.getTarget();
                    boolean removed = nothing.remove(target);
                    if (!removed) {
                        log.error("Did not remove {} from nothing list [{}]", target.toShortString(), XivEntity.fmtShortList(nothing));
                    }
                });
                List<XivPlayerCharacter> unstablePlayers = unstable.stream()
                        .map(BuffApplied::getTarget)
                        .map(XivPlayerCharacter.class::cast)
                        .sorted(getPangenesisPrio().getComparator())
                        .toList();
                List<XivPlayerCharacter> shortLightPlayers = lightShort.stream()
                        .map(BuffApplied::getTarget)
                        .map(XivPlayerCharacter.class::cast)
                        .sorted(getPangenesisPrio().getComparator())
                        .toList();
                List<XivPlayerCharacter> longLightPlayers = lightLong.stream()
                        .map(BuffApplied::getTarget)
                        .map(XivPlayerCharacter.class::cast)
                        .sorted(getPangenesisPrio().getComparator())
                        .toList();
                List<XivPlayerCharacter> shortDarkPlayers = darkShort.stream()
                        .map(BuffApplied::getTarget)
                        .map(XivPlayerCharacter.class::cast)
                        .sorted(getPangenesisPrio().getComparator())
                        .toList();
                List<XivPlayerCharacter> longDarkPlayers = darkLong.stream()
                        .map(BuffApplied::getTarget)
                        .map(XivPlayerCharacter.class::cast)
                        .sorted(getPangenesisPrio().getComparator())
                        .toList();
                List<XivPlayerCharacter> nothingPlayers = nothing.stream()
                                .sorted(getPangenesisPrio().getComparator())
                                        .toList();
                log.info("Pangenesis: Unstable [{}], ShortLight [{}], LongLight [{}], ShortDark [{}], LongDark [{}], Nothing [{}]",
                        XivEntity.fmtShortList(unstablePlayers),
                        XivEntity.fmtShortList(shortLightPlayers),
                        XivEntity.fmtShortList(longLightPlayers),
                        XivEntity.fmtShortList(shortDarkPlayers),
                        XivEntity.fmtShortList(longDarkPlayers),
                        XivEntity.fmtShortList(nothingPlayers));
                if(unstablePlayers.size() != 2 || shortLightPlayers.size() != 1 || longLightPlayers.size() != 1 || shortDarkPlayers.size() != 1 || longDarkPlayers.size() != 1 || nothingPlayers.size() != 2) {
                    log.warn("Failure in pangenesis AM!");
                    return;
                }
                s.setParam("unstables", unstablePlayers);
                s.setParam("shortLights", shortLightPlayers);
                s.setParam("longLights", longLightPlayers);
                s.setParam("shortDarks", shortDarkPlayers);
                s.setParam("longDarks", longDarkPlayers);
                s.setParam("nothings", nothingPlayers);
                //Mark players
                if(getPangenesisAmEnable().get()) {
                    s.waitMs(100);
                    MultiSlotAutoMarkHandler<PangenesisAssignment> handler = new MultiSlotAutoMarkHandler<>(s::accept, getPangenesisAMSettings());
                    handler.processRange(unstablePlayers, PangenesisAssignment.Debuff1, PangenesisAssignment.Debuff2);
                    handler.processRange(nothingPlayers, PangenesisAssignment.Nothing1, PangenesisAssignment.Nothing2);
                    handler.process(PangenesisAssignment.ShortLight, shortLightPlayers.get(0));
                    handler.process(PangenesisAssignment.LongLight, longLightPlayers.get(0));
                    handler.process(PangenesisAssignment.ShortDark, shortDarkPlayers.get(0));
                    handler.process(PangenesisAssignment.LongDark, longDarkPlayers.get(0));
                }

                s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x831A));
                if(getPangenesisAmEnable().get()) {
                    s.accept(new ClearAutoMarkRequest());
                }
            });

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final JobSortSetting groupPrioJobSort;
    private final JobSortOverrideSetting pangenesisPrio;
    private final MultiSlotAutomarkSetting<PangenesisAssignment> pangenesisAMSettings;
    private final BooleanSetting pangenesisAmEnable;

    private XivState getState() {
        return state;
    }

    private StatusEffectRepository getBuffs() {
        return buffs;
    }

    public JobSortSetting getGroupPrioJobSort() {
        return groupPrioJobSort;
    }

    public MultiSlotAutomarkSetting<PangenesisAssignment> getPangenesisAMSettings() {
        return pangenesisAMSettings;
    }

    public BooleanSetting getPangenesisAmEnable() {
        return pangenesisAmEnable;
    }

    public JobSortOverrideSetting getPangenesisPrio() {
        return pangenesisPrio;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.P12S);
    }
}
