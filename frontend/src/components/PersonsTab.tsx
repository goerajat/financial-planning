import React, { useState } from 'react';
import { usePlan } from '../context/PlanContext';
import { Person } from '../types';
import './Tab.css';

const PersonsTab: React.FC = () => {
  const { persons, addPerson, updatePerson, deletePerson } = usePlan();
  const [editIndex, setEditIndex] = useState<number | null>(null);
  const [formData, setFormData] = useState<Person>({ name: '', yearOfBirth: 1980 });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editIndex !== null) {
      updatePerson(editIndex, formData);
      setEditIndex(null);
    } else {
      addPerson(formData);
    }
    setFormData({ name: '', yearOfBirth: 1980 });
  };

  const handleEdit = (index: number) => {
    setEditIndex(index);
    setFormData(persons[index]);
  };

  const handleCancel = () => {
    setEditIndex(null);
    setFormData({ name: '', yearOfBirth: 1980 });
  };

  return (
    <div className="tab-content">
      <h2>Persons</h2>

      <form onSubmit={handleSubmit} className="form-row">
        <input
          type="text"
          placeholder="Name"
          value={formData.name}
          onChange={e => setFormData({ ...formData, name: e.target.value })}
          required
        />
        <input
          type="number"
          placeholder="Year of Birth"
          value={formData.yearOfBirth}
          onChange={e => setFormData({ ...formData, yearOfBirth: parseInt(e.target.value) })}
          required
        />
        <button type="submit">{editIndex !== null ? 'Update' : 'Add'}</button>
        {editIndex !== null && (
          <button type="button" onClick={handleCancel}>Cancel</button>
        )}
      </form>

      <table className="data-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Year of Birth</th>
            <th>Current Age</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {persons.map((person, index) => (
            <tr key={index}>
              <td>{person.name}</td>
              <td>{person.yearOfBirth}</td>
              <td>{person.currentAge}</td>
              <td>
                <button onClick={() => handleEdit(index)}>Edit</button>
                <button onClick={() => deletePerson(index)}>Delete</button>
              </td>
            </tr>
          ))}
          {persons.length === 0 && (
            <tr>
              <td colSpan={4} className="empty-message">No persons added yet</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default PersonsTab;
