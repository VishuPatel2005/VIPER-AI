import React from 'react';
import {View, StyleSheet, TouchableOpacity, Text} from 'react-native';
import AnimatedVisualizer from './AnimatedVisualizer';

const BottomNavigation = () => {
  return (
    <View style={styles.container}>
      {/* Contacts Icon */}
      <TouchableOpacity style={styles.navItem}>
        <View style={styles.contactsIcon}>
          <View style={styles.personIcon}>
            <View style={styles.personHead} />
            <View style={styles.personBody} />
          </View>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>1</Text>
          </View>
        </View>
      </TouchableOpacity>

      {/* Center Visualizer */}
      <View style={styles.centerNavItem}>
        <AnimatedVisualizer size={50} />
      </View>

      {/* Settings Icon */}
      <TouchableOpacity style={styles.navItem}>
        <View style={styles.settingsIcon}>
          <View style={styles.gearOuter}>
            <View style={styles.gearInner} />
            <View style={styles.gearTooth} />
            <View style={[styles.gearTooth, styles.gearTooth2]} />
            <View style={[styles.gearTooth, styles.gearTooth3]} />
            <View style={[styles.gearTooth, styles.gearTooth4]} />
          </View>
        </View>
      </TouchableOpacity>

      {/* Home Indicator */}
      <View style={styles.homeIndicator} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    backgroundColor: '#000000',
    paddingVertical: 15,
    paddingBottom: 25,
    borderTopWidth: 0.5,
    borderTopColor: '#1a1a1a',
  },
  navItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  centerNavItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  contactsIcon: {
    width: 30,
    height: 30,
    position: 'relative',
    alignItems: 'center',
    justifyContent: 'center',
  },
  personIcon: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  personHead: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#FFFFFF',
    marginBottom: 2,
  },
  personBody: {
    width: 16,
    height: 12,
    borderTopLeftRadius: 8,
    borderTopRightRadius: 8,
    borderWidth: 1.5,
    borderColor: '#FFFFFF',
    borderBottomWidth: 0,
  },
  badge: {
    position: 'absolute',
    top: -2,
    right: -2,
    width: 16,
    height: 16,
    borderRadius: 8,
    backgroundColor: '#00FF88',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
    borderColor: '#000000',
  },
  badgeText: {
    color: '#000000',
    fontSize: 10,
    fontWeight: 'bold',
  },
  settingsIcon: {
    width: 30,
    height: 30,
    alignItems: 'center',
    justifyContent: 'center',
  },
  gearOuter: {
    width: 24,
    height: 24,
    position: 'relative',
    alignItems: 'center',
    justifyContent: 'center',
  },
  gearInner: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: '#FFFFFF',
  },
  gearTooth: {
    position: 'absolute',
    width: 3,
    height: 8,
    backgroundColor: '#FFFFFF',
    top: -2,
  },
  gearTooth2: {
    transform: [{rotate: '45deg'}],
    right: 2,
  },
  gearTooth3: {
    transform: [{rotate: '90deg'}],
    right: 8,
    top: 2,
  },
  gearTooth4: {
    transform: [{rotate: '135deg'}],
    right: 2,
    top: 8,
  },
  homeIndicator: {
    position: 'absolute',
    bottom: 8,
    left: '50%',
    marginLeft: -40,
    width: 80,
    height: 4,
    backgroundColor: '#666666',
    borderRadius: 2,
  },
});

export default BottomNavigation;
